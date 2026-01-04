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
import java.util.Collections;
import java.util.List;
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

  /**
   * Returns a stream to get records from a CSV file. A new schema is created based off of the
   * partialSchema: any field encountered that is not in the partialSchema will be added to the
   * resulting schema as type String.
   *
   * @apiNote Similar to {@link java.nio.file.Files#lines(Path)}, this should be used within a
   *     try-with-resources statement or similar to ensure the stream's file is closed promptly.
   * @param csvFile the file to read CSV data from
   * @param partialSchema matching fields from the CSV will become the types specified in the
   *     schema.
   * @return A TabStream of the CSV data. The schema is a merge of the fields from the given schema
   *     and any fields from the file that were not listed in the schema.
   * @throws IOException if the file was not found or could not be opened.
   */
  public static TabStream records(Path csvFile, TabSchema partialSchema) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(csvFile.toFile()));
    CsvParserSettings settings = new CsvParserSettings();
    CsvParser parser = new CsvParser(settings);
    IterableResult<String[], ParsingContext> iterable = parser.iterate(br);
    Spliterator<String[]> iter = iterable.spliterator();
    HeadersGetter headersGetter = new HeadersGetter();
    iter.tryAdvance(headersGetter.fetcher());

    List<String> headers = headersGetter.headers();
    List<String> givenFields = partialSchema.fields().stream().map(TabField::name).toList();
    List<TabField.StrF> unlistedFields =
        headers.stream()
            .filter(Predicate.not(givenFields::contains))
            .map(TabField.StrF::optional)
            .toList();
    TabSchema resultSchema;
    if (unlistedFields.isEmpty()) {
      resultSchema = partialSchema;
    } else {
      resultSchema =
          new TabSchema(
              Stream.concat(partialSchema.fields().stream(), unlistedFields.stream()).toList());
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
            .mapToObj(
                i -> {
                  return valConverter(resultSchema.fields().get(i));
                })
            .toList();
    // Actual converter of a CSV row of Strings to a record with values converted to correct types
    Function<String[], TabRecord> toTabRecord =
        row -> {
          List<?> values =
              IntStream.range(0, outPosToInPos.length)
                  .mapToObj(
                      i -> {
                        int j = outPosToInPos[i];
                        if (j < 0) {
                          // Schema specified a field that is not found in the CSV. Keep it null.
                          // TODO: set to default value from schema (or null by "default" default)
                          return null;
                        }
                        return fieldConverters.get(i).apply(row[j]);
                      })
                  .toList();
          return new TabRecord(values);
        };
    return new CsvTabStream(
        resultSchema,
        StreamSupport.stream(iter, false).onClose(uncheckedCloser(br)).map(toTabRecord));
  }

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
  public static TabStream records(Path csvFile) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(csvFile.toFile()));
    CsvParserSettings settings = new CsvParserSettings();
    CsvParser parser = new CsvParser(settings);
    IterableResult<String[], ParsingContext> iterable = parser.iterate(br);
    Spliterator<String[]> iter = iterable.spliterator();
    HeadersGetter headersGetter = new HeadersGetter();
    iter.tryAdvance(headersGetter.fetcher());
    List<String> headers = headersGetter.headers();
    List<? extends TabField<?>> fields = headers.stream().map(TabField.StrF::optional).toList();
    TabSchema schema = new TabSchema(fields);

    Function<String[], TabRecord> toTabRecord =
        row -> new TabRecord(Collections.unmodifiableList(Arrays.asList(row)));
    return new CsvTabStream(
        schema, StreamSupport.stream(iter, false).onClose(uncheckedCloser(br)).map(toTabRecord));
  }

  public static String write(Path dest, TabSchema schema, Stream<TabRecord> rows)
      throws IOException {
    List<String> headers = schema.fields().stream().map(TabField::name).toList();
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

  private static Function<TabRecord, TabRecord> createCsvConverter(TabSchema schema) {
    List<? extends Function<String, ?>> valConvs =
        schema.fields().stream().map(Csvs::valConverter).toList();
    int size = valConvs.size();
    return (TabRecord sourceRow) -> {
      List<?> destVals =
          IntStream.range(0, size)
              .mapToObj(i -> valConvs.get(i).apply((String) sourceRow.get(i)))
              .toList();
      return new TabRecord(destVals);
    };
  }

  private static <U> Function<String, U> valConverter(TabField<U> field) {
    return switch (field) {
      case TabField.StrF f -> (Function<String, U>) orOpt(Function.identity(), f);
      case TabField.IntF f -> (Function<String, U>) orOpt(Integer::valueOf, f);
      case TabField.LongF f -> (Function<String, U>) orOpt(Long::valueOf, f);
      case TabField.FloatF f -> (Function<String, U>) orOpt(Float::valueOf, f);
      case TabField.DoubleF f -> (Function<String, U>) orOpt(Double::valueOf, f);
      case TabField.BooleanF f -> (Function<String, U>) orOpt(Boolean::valueOf, f);
    };
  }

  private static <U> Function<String, U> orOpt(Function<String, U> fromString, TabField<U> field) {
    U defVal = field.opt().defVal();
    // If field is optional, then when null is encountered, provide the default value
    if (field.isOptional()) {
      return source -> {
        if (source == null) {
          return defVal;
        }
        return fromString.apply(source);
      };
    }
    // otherwise if required, then when null is encountered, throw exception
    return source -> {
      if (source == null) {
        throw new IllegalArgumentException("Not optional, but was null: " + field);
      }
      return fromString.apply(source);
    };
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

  private record CsvTabStream(TabSchema schema, Stream<TabRecord> stream) implements TabStream {
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
