package com.redsaz.lognition.convert;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.univocity.parsers.common.IterableResult;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Csvs {
  // Do not instantiate utility classes
  private Csvs() {}

  private static final TabValueException SKIP_RECORD =
      new TabValueException("Internal marker to skip record due to bad value.", null);

  /** See {@link SimpleReadOption} and {@link ReadErrorHandler} for more information. */
  public sealed interface ReadOption permits SimpleReadOption, ReadErrorHandler {}

  public enum ErrorReason {
    /** No error for the element. */
    GOOD,
    /** A value is required for the element, but none was given. */
    MISSING_REQUIRED_VALUE,
    ErrorReason,
    /** A value was found for the element, but is not of the expected type. */
    MISTYPED_VALUE
  }

  public sealed interface ErrorItem permits ErrorItem.MissingValue, ErrorItem.MistypedValue {
    TabSchema<?> schema();

    record MissingValue(TabSchema<?> schema) implements ErrorItem {}
    ;

    record MistypedValue(TabSchema<?> schema, String mistypedValue) implements ErrorItem {}
    ;
  }

  /** Any unhandled errors while processing the data will be reported to this handler. */
  public non-sealed interface ReadErrorHandler extends ReadOption {
    <U> ErrorAction<U> handleError(TabValueException ex);
  }

  public static ReadErrorHandler handleErrors(Function<TabValueException, ErrorAction<?>> error) {
    return new ReadErrorHandler() {
      @Override
      public ErrorAction<?> handleError(TabValueException ex) {
        return error.apply(ex);
      }
    };
  }

  public enum SimpleReadOption implements ReadOption {
    /**
     * If a value's type does not match the type specified in the schema, and the type is not
     * required, ignore it as if the value wasn't provided at all.
     */
    IGNORE_MISTYPED,
    /**
     * if a value's type does not match the type specified in the schema, skip the entire record.
     */
    SKIP_MISTYPED,
    /**
     * If a value's type does not match the type specified in the schema, fail the entire read
     * operation.
     */
    FAIL_MISTYPED,

    /**
     * Columns from the file, not specified in the schema, will be skipped (will not be added to the
     * schema).
     */
    IGNORE_UNKNOWN,

    /**
     * Columns from the file, not specified in the schema, will be added to the schema as a string.
     */
    ADD_UNKNOWN
  }

  /**
   * The action the reader should perform for a given error. One of:
   *
   * <ul>
   *   <li>Recover - Use the provided fixed value and continue reading
   *   <li>Skip - Skip over the failed record but read further records
   *   <li>Fail - Throw an error, do not read any more records
   * </ul>
   */
  public static class ErrorAction<U> {
    private enum ActionType {
      RECOVER,
      SKIP,
      FAIL
    }

    private final ActionType type;
    private final U recovered;

    private ErrorAction(ActionType type, U recovered) {
      this.type = type;
      this.recovered = recovered;
    }

    public static <U> ErrorAction<U> recover(U recovered) {
      return new ErrorAction<U>(ActionType.RECOVER, recovered);
    }

    private static final ErrorAction<?> SKIP = new ErrorAction<>(ActionType.SKIP, null);

    public static ErrorAction<?> skip() {
      return SKIP;
    }

    private static final ErrorAction<?> FAIL = new ErrorAction<>(ActionType.FAIL, null);

    public static ErrorAction<?> fail() {
      return FAIL;
    }
  }

  private static boolean hasSimpleReadOption(SimpleReadOption opt, ReadOption[] opts) {
    if (opts == null) {
      return false;
    }
    return Arrays.stream(opts).anyMatch(o -> o == opt);
  }

  private static SimpleReadOption findMistypeOption(ReadOption... opts) {
    SimpleReadOption mistyped = null;
    for (ReadOption opt : opts) {
      if (opt == SimpleReadOption.IGNORE_MISTYPED
          || opt == SimpleReadOption.SKIP_MISTYPED
          || opt == SimpleReadOption.FAIL_MISTYPED) {
        if (mistyped == null) {
          mistyped = (SimpleReadOption) opt;
        } else {
          throw new TabException("Conflicting options used: %s and %s".formatted(mistyped, opt));
        }
      }
    }
    return mistyped;
  }

  private static ReadErrorHandler findErrorHandler(ReadOption... opts) {
    for (ReadOption opt : opts) {
      if (opt instanceof ReadErrorHandler o) {
        return o;
      }
    }
    return null;
  }

  private static <U> U handleException(TabValueException ex, ReadErrorHandler errHandler) {
    if (errHandler == null) {
      throw ex;
    }

    ErrorAction<U> action = errHandler.handleError(ex);
    if (action == null) {
      throw ex;
    }
    switch (action.type) {
      case RECOVER -> {
        // If the recovery itself does not conform to the schema, throw an exception.
        if (action.recovered == null && ex.schema().isRequired()) {
          TabValueException e = new TabValueRequiredException(ex.schema());
          e.addSuppressed(ex);
          throw e;
        } else if (action.recovered != null
            && !action.recovered.getClass().isAssignableFrom(ex.schema().type())) {
          TabValueException e = new TabValueMistypedException(ex.schema(), action.recovered);
          e.addSuppressed(ex);
          throw e;
        }
      }
      case FAIL -> throw ex;
      case SKIP -> throw SKIP_RECORD;
    }
    return action.recovered;
  }

  /**
   * A function that takes in a CSV row as an array of strings, and deserializes into a type.
   *
   * @param <U> The type to deserialize into.
   */
  public interface Deserializer<U> extends Function<String[], Stream<U>> {}

  /**
   * A function that takes in the headers of a CSV file and return the appropriate deserializer
   * function.
   *
   * @param <U> The type to deserialize into.
   */
  public interface DeserializerPlanner<U> extends Function<List<String>, Deserializer<U>> {}

  /**
   * Fetch deserialized records from a CSV file, allowing the user to set up a deserializer based on
   * the headers of the data.
   *
   * @apiNote Similar to {@link java.nio.file.Files#lines(Path)}, this should be used within a
   *     try-with-resources statement or similar to ensure the stream's file is closed promptly.
   * @param csvFile The CSV data to load.
   * @param planner Receives the headers and returns a deserializer that can deserialize each row.
   * @return a stream of records, deserialized
   * @param <U> the type each record is deserialized into
   * @throws IOException when encountering an error reading the file.
   */
  public static <U> Stream<U> recordsUsing(Path csvFile, DeserializerPlanner<U> planner)
      throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(csvFile.toFile()));
    CsvParserSettings settings = new CsvParserSettings();
    CsvParser parser = new CsvParser(settings);
    IterableResult<String[], ParsingContext> iterable = parser.iterate(br);
    Spliterator<String[]> iter = iterable.spliterator();
    HeadersGetter headersGetter = new HeadersGetter();
    iter.tryAdvance(headersGetter.fetcher());

    List<String> headers = headersGetter.headers();
    Stream<String[]> rowStream =
        StreamSupport.stream(iter, false).onClose(uncheckedCloser(br)).filter(Objects::nonNull);

    Deserializer<U> deser = planner.apply(headers);
    return rowStream.flatMap(deser);
  }

  private static class TabRecordPlanner implements DeserializerPlanner<TabRecord> {

    private final TabSchema.StructS given;
    private final boolean addUnknown;
    private final boolean ignoreMistyped;
    private final ReadErrorHandler errorHandler;
    private TabSchema.StructS resultSchema;

    public TabRecordPlanner(
        TabSchema.StructS schema,
        boolean addUnknown,
        boolean ignoreMistyped,
        ReadErrorHandler errorHandler) {
      this.given = schema;
      this.addUnknown = addUnknown;
      this.ignoreMistyped = ignoreMistyped;
      this.errorHandler = errorHandler;
    }

    @Override
    public Deserializer<TabRecord> apply(List<String> headers) {
      if (addUnknown) {
        List<String> givenFields = given.fields().stream().map(TabSchema::name).toList();
        List<TabSchema.StrS> unlistedFields =
            headers.stream()
                .filter(Predicate.not(givenFields::contains))
                .map(TabSchema.StrS::optional)
                .toList();
        if (unlistedFields.isEmpty()) {
          resultSchema = given;
        } else {
          resultSchema =
              new TabSchema.StructS(
                  given.name(),
                  Stream.concat(given.fields().stream(), unlistedFields.stream()).toList());
        }
      } else {
        resultSchema = given;
      }

      // The resulting record may have a different order of fields than what the input CSV has,
      // depending on how the schema was specified.

      // index is the position of the output schema, value is which header it maps to.
      int[] outPosToInPos =
          IntStream.range(0, resultSchema.fields().size())
              .map(
                  i -> {
                    String name = resultSchema.fields().get(i).name();
                    return headers.indexOf(name);
                  })
              .toArray();
      // Each converter is how to convert a string into the field type per position
      List<? extends Function<String, ?>> fieldConverters =
          IntStream.range(0, outPosToInPos.length)
              .mapToObj(i -> valConverter(resultSchema.fields().get(i), ignoreMistyped))
              .toList();
      // Actual converter of a CSV row of Strings to a record with values converted to correct types
      return row -> {
        try {
          List<?> values =
              IntStream.range(0, outPosToInPos.length)
                  .mapToObj(
                      i -> {
                        try {
                          // TODO: It seems like this and the fieldConverters could be combined,
                          // so that ifs don't need to happen every row.
                          int j = outPosToInPos[i];
                          if (j < 0) {
                            // Schema specified a field that is not found in the CSV. Use the
                            // default value, or report error if the field is required.
                            TabSchema<?> field = given.fields().get(i);
                            if (field.isRequired()) {
                              throw new TabValueRequiredException(field);
                            }
                            return field.defVal();
                          }
                          return fieldConverters.get(i).apply(row[j]);
                        } catch (TabValueException ex) {
                          return handleException(ex, errorHandler);
                        }
                      })
                  .toList();
          return Stream.of(new TabRecord(values));
        } catch (TabValueException ex) {
          if (ex == SKIP_RECORD) {
            return Stream.empty();
          }
          throw ex;
        }
      };
    }

    public TabSchema.StructS getSchema() {
      return resultSchema;
    }
  }

  /**
   * Returns a stream to get records from a CSV file. Options can configure how to read from the CSV
   * file, for example supplying a {@link ReadErrorHandler} can allow custom recovery of a record if
   * encountered. Some options are mutually exclusive and will result in an exception if conflicting
   * options are provided.
   *
   * @apiNote Similar to {@link java.nio.file.Files#lines(Path)}, this should be used within a
   *     try-with-resources statement or similar to ensure the stream's file is closed promptly.
   * @param csvFile the file to read CSV data from
   * @param schema matching fields from the CSV will become the types specified in the schema.
   * @param opts The options for reading data.
   * @return A TabStream of the CSV data. The schema is a merge of the fields from the given schema
   *     and any fields from the file that were not listed in the schema.
   * @throws IOException if the file was not found or could not be opened.
   */
  public static TabStream records(Path csvFile, TabSchema.StructS schema, ReadOption... opts)
      throws IOException {
    TabRecordPlanner planner =
        new TabRecordPlanner(
            schema,
            hasSimpleReadOption(SimpleReadOption.ADD_UNKNOWN, opts),
            hasSimpleReadOption(SimpleReadOption.IGNORE_MISTYPED, opts),
            findErrorHandler(opts));
    Stream<TabRecord> stream = recordsUsing(csvFile, planner);
    return new CsvTabStream(planner.getSchema(), stream);
  }

  private static final TabSchema.StructS EMPTY_STRUCT = new TabSchema.StructS("Record", List.of());

  /**
   * Returns a stream to get records from a CSV file and auto-generates a schema where all fields
   * are of type String.
   *
   * @apiNote Similar to {@link java.nio.file.Files#lines(Path)}, this should be used within a
   *     try-with-resources statement or similar to ensure the stream's file is closed promptly.
   * @param csvFile the file to read CSV data from
   * @return A CsvStream which has the headers and the stream to read the lines from.
   * @throws IOException if the file was not found or could not be opened.
   */
  public static TabStream recordsAsStrings(Path csvFile) throws IOException {
    TabRecordPlanner planner = new TabRecordPlanner(EMPTY_STRUCT, true, false, null);
    Stream<TabRecord> stream = recordsUsing(csvFile, planner);
    return new CsvTabStream(planner.getSchema(), stream);
  }

  public static String write(Path dest, TabSchema.StructS schema, Stream<TabRecord> rows)
      throws IOException {
    List<String> headers = schema.fields().stream().map(TabSchema::name).toList();
    return writeRecords(dest, headers, rows);
  }

  public static String writeRecords(Path dest, List<String> headers, Stream<TabRecord> rows)
      throws IOException {
    try (HashingOutputStream hos =
        new HashingOutputStream(
            Hashing.sha256(), new BufferedOutputStream(new FileOutputStream(dest.toFile())))) {
      try (BufferedWriter bw =
          new BufferedWriter(new OutputStreamWriter(hos, StandardCharsets.UTF_8))) {
        CsvWriter writer = null;
        try {
          writer = new CsvWriter(bw, new CsvWriterSettings());
          if (headers != null && !headers.isEmpty()) {
            // Only write the headers if there are any.
            writer.writeHeaders(headers);
          }
          rows.map(row -> row.values().stream().map(Csvs::stringify).toList())
              .forEach(writer::writeRow);
        } finally {
          if (writer != null) {
            writer.close(); // Looks like it could be put in try-with-resources, but nope.
          }
        }
      }
      return hos.hash().toString();
    }
  }

  private static <U> Function<String, U> valConverter(TabSchema<U> field, boolean ignoreMistyped) {
    return switch (field) {
      case TabSchema.StrS f -> (Function<String, U>) orOpt(Function.identity(), f, ignoreMistyped);
      case TabSchema.IntS f -> (Function<String, U>) orOpt(Integer::valueOf, f, ignoreMistyped);
      case TabSchema.LongS f -> (Function<String, U>) orOpt(Long::valueOf, f, ignoreMistyped);
      case TabSchema.FloatS f -> (Function<String, U>) orOpt(Float::valueOf, f, ignoreMistyped);
      case TabSchema.DoubleS f -> (Function<String, U>) orOpt(Double::valueOf, f, ignoreMistyped);
      case TabSchema.BooleanS f -> (Function<String, U>) orOpt(Boolean::valueOf, f, ignoreMistyped);
      case TabSchema.UnionS f -> (Function<String, U>) orOpt(unionFromCsv(f), f, ignoreMistyped);
      case TabSchema.StructS f ->
          throw new TabValueException(
              "StructS is not yet supported as a field of a StructS when reading from CSV.", field);
    };
  }

  private static <U> Function<String, U> orOpt(
      Function<String, U> fromString, TabSchema<U> field, boolean ignoreMistyped) {
    U defVal = field.opt().defVal();
    // If field is optional, then when null is encountered, provide the default value
    if (field.isOptional()) {
      if (ignoreMistyped) {
        // If a value of an unexpected type was given, toss it out and use the default value.
        return source -> {
          if (source == null) {
            return defVal;
          }
          try {
            return fromString.apply(source);
          } catch (NumberFormatException ex) {
            return defVal;
          }
        };
      } else {
        // Try to convert, and allow mistyped exceptions
        return source -> {
          try {
            if (source == null) {
              return defVal;
            }
            return fromString.apply(source);
          } catch (RuntimeException ex) {
            throw new TabValueMistypedException(field, source);
          }
        };
      }
    }
    // otherwise if required, then when null is encountered, throw exception
    return source -> {
      if (source == null) {
        throw new TabValueRequiredException(field);
      }
      // Try to convert, and allow mistyped exceptions.
      try {
        return fromString.apply(source);
      } catch (RuntimeException ex) {
        throw new TabValueMistypedException(field, source);
      }
    };
  }

  /**
   * CSV files don't really have types, per se, so this does a best guess fit. The order of types
   * specified in the union do not matter. For the "worst case" union that could be one of any
   * String, long, double, int, float, and boolean, the priorities and rules from highest to lowest
   * are:
   *
   * <ul>
   *   <li>int - if no E and no decimal point and within INT_MIN and INT_MAX
   *   <li>long - if no E and no decimal point and within LONG_MIN and LONG_MAX
   *   <li>double - if an E and/or decimal point
   *   <li>float - if an E and/or decimal point (yes this means if union has flaot and double,
   *       double always wins)
   *   <li>boolean - if case-insensitive "true" or "false"
   *   <li>string - when nothing else matched
   * </ul>
   *
   * @param union The union containing the types to try to deserialize.
   * @return The value, as a type in the union.
   */
  private static <U> Function<String, U> unionFromCsv(TabSchema.UnionS union) {
    boolean hasInt = union.types().contains(Integer.class);
    boolean hasLong = union.types().contains(Long.class);
    boolean hasDouble = union.types().contains(Double.class);
    boolean hasFloat = union.types().contains(Float.class);
    boolean hasBoolean = union.types().contains(Boolean.class);
    boolean hasString = union.types().contains(String.class);
    Function<String, Object> iGetter;
    if (hasInt && hasLong) {
      iGetter =
          source -> {
            if (!integery(source)) {
              return null;
            }
            try {
              long val = Long.valueOf(source);
              if (hasInt && val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                return (int) val;
              }
              return val;
            } catch (NumberFormatException ex) {
              return null;
            }
          };
    } else if (hasLong) {
      iGetter =
          source -> {
            if (!integery(source)) {
              return null;
            }
            try {
              return Long.valueOf(source);
            } catch (NumberFormatException ex) {
              return null;
            }
          };
    } else if (hasInt) {
      iGetter =
          source -> {
            if (!integery(source)) {
              return null;
            }
            try {
              return Integer.valueOf(source);
            } catch (NumberFormatException ex) {
              return null;
            }
          };
    } else {
      iGetter = null;
    }
    Function<String, Object> fGetter;
    if (hasDouble) {
      fGetter =
          source -> {
            if (!floatish(source)) {
              return null;
            }
            try {
              return Double.valueOf(source);
            } catch (NumberFormatException ex) {
              return null;
            }
          };
    } else if (hasFloat) {
      fGetter =
          source -> {
            if (!floatish(source)) {
              return null;
            }
            try {
              return Float.valueOf(source);
            } catch (NumberFormatException ex) {
              return null;
            }
          };
    } else {
      fGetter = null;
    }
    Function<String, Boolean> bGetter;
    if (hasBoolean) {
      bGetter =
          source -> {
            // This looks like it could be simplified, but we
            // need it to only deal with "tRuE" and "FaLsE" strings
            // (Boolean.parseBoolean(str) will treat "fAlSe", "banana", "yes", "no", "maybe", etc
            // all as false. We only want the "fAlSe" case to be treated as false, skip the rest)
            if (Boolean.parseBoolean(source)) {
              return true;
            } else if ("false".equalsIgnoreCase(source)) {
              return false;
            }
            return null;
          };
    } else {
      bGetter = null;
    }
    Function<String, String> sGetter;
    if (hasString) {
      sGetter = Function.identity();
    } else {
      sGetter = null;
    }
    List<Function<String, ? extends Object>> getters =
        Stream.of(iGetter, fGetter, bGetter, sGetter).filter(Objects::nonNull).toList();

    return (String source) ->
        (U)
            getters.stream()
                .map(getter -> getter.apply(source))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
  }

  private static Runnable uncheckedCloser(Closeable closeable) {
    return () -> {
      try {
        closeable.close();
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    };
  }

  /**
   * @return the toString() result if the object is non-null, or a null string if the object is
   *     null.
   */
  private static String stringify(Object obj) {
    if (obj == null) {
      return null;
    }
    return obj.toString();
  }

  /**
   * @return true if a string *may* be able to be safely interpreted as a long/int, false if not.
   */
  private static boolean integery(String str) {
    if (str == null) {
      return false;
    }
    int len = str.length();
    int start = 0;
    char c = str.charAt(start);
    if (c == '+' || c == '-') {
      ++start;
    }
    for (int i = start; i < len; ++i) {
      c = str.charAt(i);
      if (!Character.isDigit(c)) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return true if a string *may* be able to be safely interpreted as a float/double (including
   *     NaN), false if not.
   */
  private static boolean floatish(String str) {
    // This method is purposely simplistic. It'll reject expected stuff like "bob", "", null, "NAN",
    // "nan" from being considered floaty,
    // and will allow "1", "1.", ".1", "-1", "+1", "1E1", "NaN" as floaty, but MAY allow unexpected
    // strings like "..1..", "--1", "1-+", "1EE-1.2-" as being floaty, so try-catch is still needed.
    if (str == null || str.isBlank()) {
      return false;
    } else if ("NaN".equals(str)) {
      return true;
    }
    int len = str.length();
    for (int i = 0; i < len; ++i) {
      char c = str.charAt(i);
      if (c >= '0' && c <= '9') {
        continue;
      } else if (c == 'e' || c == 'E' || c == '.' || c == '-' || c == '+') {
        continue;
      }
      return false;
    }
    return true;
  }

  private record CsvTabStream(TabSchema.StructS schema, Stream<TabRecord> stream)
      implements TabStream {
    @Override
    public void close() throws IOException {
      stream().close();
    }
  }

  private static class HeadersGetter {
    private List<String> headers = List.of();

    public Consumer<? super String[]> fetcher() {
      return (row) -> headers = List.copyOf(Arrays.asList(row));
    }

    public List<String> headers() {
      return headers;
    }
  }
}
