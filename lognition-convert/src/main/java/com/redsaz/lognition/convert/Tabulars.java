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
          List<Object> tabVals =
              IntStream.range(0, schema.fields().size())
                  .mapToObj(
                      i ->
                          (Object)
                              switch (genRec.get(i)) {
                                case null -> null;
                                case Utf8 v -> v.toString();
                                case Integer v -> v;
                                case Long v -> v;
                                case Float v -> v;
                                case Double v -> v;
                                case Boolean v -> v;
                                default ->
                                    throw new IllegalArgumentException(
                                        "Not a compatible type for TabRecord: "
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

  //  /**
  //   * Given a source TabStream of TabRecords of all TabVals, and a destination schema, creates a
  //   * TabStream that is compliant with the destination schema.
  //   *
  //   * @param sourceSchema the names of each field as they appear in each row of the row stream
  //   * @param sourceRowStream the stream of rows, each row with the same number of fields as the
  // count
  //   *     of fieldNames
  //   * @param destSchema the schema of the destination tabular data stream.
  //   * @return a tabular data stream using destSchema.
  //   */
  //  public static TabStream convert(
  //      TabSchema sourceSchema, Stream<TabRecord> sourceRowStream, TabSchema destSchema) {
  //    // TODO This seems like we could remove Csvs and just have Tabulars, right?
  //    Function<TabRecord, TabRecord> converter = createCsvConverter(destSchema);
  //    Stream<TabRecord> tabRowStream = sourceRowStream.map(converter);
  //    return new ReaderTabStream(destSchema, tabRowStream);
  //  }
  //
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
                    .forEach(i -> rec.put(i, tabRec.values().get(i)));
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

  private static Schema toAvroSchema(TabSchema schema) {
    SchemaBuilder.FieldAssembler<Schema> builder =
        SchemaBuilder.builder().record("TabRecord").fields();
    schema
        .fields()
        .forEach(
            field -> {
              builder.name(field.name());
              switch (field) {
                case TabField.StrF f -> builder.requiredString(field.name());
                case TabField.IntF f -> builder.requiredInt(field.name());
                case TabField.LongF f -> builder.requiredLong(field.name());
                case TabField.FloatF f -> builder.requiredFloat(field.name());
                case TabField.DoubleF f -> builder.requiredDouble(field.name());
                case TabField.BooleanF f -> builder.requiredBoolean(field.name());
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
