package com.redsaz.lognition.convert;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;

/**
 * Utility for reading and writing tabular data.
 *
 * <p>There's all sorts of tabular data formats, CSV/TSV for example, or JSON (If an array of
 * objects with no inner objects) / JSONL (If each object contains no inner object),
 */
public class Tabulars {

  // Do not instantiate utility classes
  private Tabulars() {}

  /**
   * Returns a stream to get tabular records from a file.
   *
   * @apiNote Similar to {@link java.nio.file.Files#lines(Path)}, this should be used within a
   *     try-with-resources statement or similar to ensure the stream's file is closed promptly.
   * @param tabFile the file to read tabular data from
   * @return A CsvStream which has the headers and the stream to read the lines from.
   * @throws IOException if the file was not found or could not be opened.
   */
  public static TabStream records(Path tabFile) throws IOException {
    DataFileReader<GenericRecord> reader =
        new DataFileReader<>(tabFile.toFile(), new GenericDatumReader<>());
    TabSchema schema = TabSchema.ofAvro(reader.getSchema());

    Function<GenericRecord, TabRecord> converter =
        genRec -> {
          List<TabVal> tabVals =
              IntStream.range(0, schema.fields().size())
                  .mapToObj(
                      i ->
                          (TabVal)
                              switch (genRec.get(i)) {
                                case null -> TabVal.Null.of();
                                case Utf8 v -> new TabVal.String(v.toString());
                                case Integer v -> new TabVal.Int(v);
                                case Long v -> new TabVal.Long(v);
                                case Float v -> new TabVal.Float(v);
                                case Double v -> new TabVal.Double(v);
                                case Boolean v -> new TabVal.Boolean(v);
                                default ->
                                    throw new IllegalArgumentException(
                                        "Count not convert value into TabVal: "
                                            + genRec.get(i).getClass()
                                            + " for field: "
                                            + schema.fields().get(i).name());
                              })
                  .toList();
          return new TabRecord(tabVals);
        };
    Stream<TabRecord> stream =
        StreamSupport.stream(reader.spliterator(), false)
            .onClose(uncheckedCloser(reader))
            .map(converter);

    return new ReaderTabStream(schema, stream);
  }

  /**
   * Given a list of field names in the order they appear in each row in the source row stream, and
   * a destination schema, creates a TabStream that is compliant with the destination schema.
   *
   * @param sourceFields the names of each field as they appear in each row of the row stream
   * @param sourceRowStream the stream of rows, each row with the same number of fields as the count
   *     of fieldNames
   * @param destSchema the schema of the destination tabular data stream.
   * @return a tabular data stream using destSchema.
   */
  public static TabStream convert(
      List<String> sourceFields, Stream<List<String>> sourceRowStream, TabSchema destSchema) {
    // TODO This seems like we could remove Csvs and just have Tabulars, right?
    Function<List<String>, TabRecord> converter = createCsvConverter(destSchema);
    Stream<TabRecord> tabRowStream = sourceRowStream.map(converter);
    return new ReaderTabStream(destSchema, tabRowStream);
  }

  /**
   * Given a list of field names in the order they appear in each row in the row stream, creates a
   * TabStream where all fields are of type String.
   *
   * @param fieldNames the names of each field as they appear in each row of the row stream
   * @param rowStream the stream of rows, each row with the same number of fields as the count of
   *     fieldNames
   * @return Essentially a CsvStream in TabStream form. It seems like a CsvStream is just a special
   *     case of TabStream, no?
   */
  public static TabStream convert(List<String> fieldNames, Stream<List<String>> rowStream) {
    // TODO This seems like we could remove Csvs and just have Tabulars, right?

    // A CsvStream is a TabStream but everything is a String. So we'll "convert" it to a TabStream
    // by making a schema of all stream types.
    SchemaBuilder.FieldAssembler<Schema> builder =
        SchemaBuilder.builder().record("TabRecord").fields();
    fieldNames.stream().forEach(builder::optionalString);
    Schema avroSchema = builder.endRecord();

    TabSchema tabSchema = TabSchema.ofAvro(avroSchema);
    Function<List<String>, TabRecord> converter = createCsvConverter(tabSchema);
    Stream<TabRecord> tabRowStream = rowStream.map(converter);
    return new ReaderTabStream(tabSchema, tabRowStream);
  }

  //
  //  /**
  //   * Given one TabStream, convert into another TabStream if the two schemas are compatible.
  //   *
  //   * @param source the origin tabular data stream.
  //   * @param destSchema the schema of the destination tabular data stream.
  //   * @return a tabular data stream using destSchema.
  //   */
  //  public static TabStream convert(TabStream source, TabSchema destSchema) {
  //    // Huh? I'm not sure what I was thinking with this. Conversion of records is complex.
  //  }

  public static String write(Path dest, TabSchema schema, Stream<TabRecord> rows)
      throws IOException {
    try (HashingOutputStream hos =
        new HashingOutputStream(
            Hashing.sha256(), new BufferedOutputStream(new FileOutputStream(dest.toFile())))) {
      try (DataFileWriter<GenericRecord> dataFileWriter =
          new DataFileWriter<>(new GenericDatumWriter<>())) {
        Schema avroSchema = toAvroSchema(schema);

        dataFileWriter.create(avroSchema, hos);

        Consumer<TabRecord> writeRecord =
            tabRec -> {
              try {
                GenericRecord rec = new GenericData.Record(avroSchema);
                IntStream.range(0, schema.fields().size())
                    .forEach(i -> rec.put(i, tabRec.values().get(i).get()));
                dataFileWriter.append(rec);
              } catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
            };
        rows.forEach(writeRecord);
      }
      return hos.hash().toString();
    }
  }

  private static Function<List<String>, TabRecord> createCsvConverter(TabSchema schema) {
    List<Function<String, TabVal>> valConvs =
        schema.fields().stream().map(Tabulars::valConverter).toList();
    int size = valConvs.size();
    ;
    return (List<String> row) -> {
      List<TabVal> converteds =
          IntStream.range(0, size).mapToObj(i -> valConvs.get(i).apply(row.get(i))).toList();
      return new TabRecord(converteds);
    };
  }

  private static Function<String, TabVal> valConverter(TabField field) {
    return switch (field) {
      case TabField.String f -> TabVal.String::new;
      case TabField.Int f -> (String val) -> new TabVal.Int(Integer.parseInt(val));
      case TabField.Long f -> (String val) -> new TabVal.Long(Long.parseLong(val));
      case TabField.Float f -> (String val) -> new TabVal.Float(Float.parseFloat(val));
      case TabField.Double f -> (String val) -> new TabVal.Double(Double.parseDouble(val));
      case TabField.Boolean f -> (String val) -> new TabVal.Boolean(Boolean.parseBoolean(val));
    };
  }

  private static Schema toAvroSchema(TabSchema schema) {
    SchemaBuilder.FieldAssembler<Schema> builder =
        SchemaBuilder.builder().record("TabRecord").fields();
    schema
        .fields()
        .forEach(
            field -> {
              builder.name(field.name());
              switch (field) {
                case TabField.String f -> builder.requiredString(field.name());
                case TabField.Int f -> builder.requiredInt(field.name());
                case TabField.Long f -> builder.requiredLong(field.name());
                case TabField.Float f -> builder.requiredFloat(field.name());
                case TabField.Double f -> builder.requiredDouble(field.name());
                case TabField.Boolean f -> builder.requiredBoolean(field.name());
              }
            });
    return builder.endRecord();
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

  private record ReaderTabStream(TabSchema schema, Stream<TabRecord> stream) implements TabStream {

    public List<String> fieldNames() {
      return schema().fields().stream().map(TabField::name).toList();
    }

    @Override
    public void close() throws IOException {
      stream().close();
    }
  }
}
