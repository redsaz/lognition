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

/** Utility for reading and writing tabular data from Avro files. */
public class Avros {

  // Do not instantiate utility classes
  private Avros() {}

  /**
   * Returns a stream to get tabular records from an avro file.
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
    TabSchema.StructS schema = TabSchema.StructS.ofAvro(reader.getSchema());

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

  /**
   * Writes a tab schema to an Avro file.
   *
   * @param dest The Avro file to write to
   * @param schema The schema for reading and writing
   * @param rows The records
   * @return The SHA256 hash of the resulting file.
   * @throws IOException if an error while writing the file.
   */
  public static String write(Path dest, TabSchema.StructS schema, Stream<TabRecord> rows)
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

  private static Schema toAvroSchema(TabSchema.StructS schema) {
    SchemaBuilder.FieldAssembler<Schema> builder =
        SchemaBuilder.builder().record("TabRecord").fields();
    schema
        .fields()
        .forEach(
            field -> {
              builder.name(field.name());
              switch (field) {
                case TabSchema.StrS f -> {
                  if (f.isOptional()) {
                    if (f.defVal() == null) {
                      builder.optionalString(f.name());
                    } else {
                      builder.nullableString(f.name(), (String) f.defVal());
                    }
                  } else {
                    builder.requiredString(field.name());
                  }
                }
                case TabSchema.IntS f -> {
                  if (f.isOptional()) {
                    if (f.defVal() == null) {
                      builder.optionalInt(f.name());
                    } else {
                      builder.nullableInt(f.name(), (Integer) f.defVal());
                    }
                  } else {
                    builder.requiredInt(field.name());
                  }
                }
                case TabSchema.LongS f -> {
                  if (f.isOptional()) {
                    if (f.defVal() == null) {
                      builder.optionalLong(f.name());
                    } else {
                      builder.nullableLong(f.name(), (Long) f.defVal());
                    }
                  } else {
                    builder.requiredLong(field.name());
                  }
                }
                case TabSchema.FloatS f -> {
                  if (f.isOptional()) {
                    if (f.defVal() == null) {
                      builder.optionalFloat(f.name());
                    } else {
                      builder.nullableFloat(f.name(), (Float) f.defVal());
                    }
                  } else {
                    builder.requiredFloat(field.name());
                  }
                }
                case TabSchema.DoubleS f -> {
                  if (f.isOptional()) {
                    if (f.defVal() == null) {
                      builder.optionalDouble(f.name());
                    } else {
                      builder.nullableDouble(f.name(), (Double) f.defVal());
                    }
                  } else {
                    builder.requiredDouble(field.name());
                  }
                }
                case TabSchema.BooleanS f -> {
                  if (f.isOptional()) {
                    if (f.defVal() == null) {
                      builder.optionalBoolean(f.name());
                    } else {
                      builder.nullableBoolean(f.name(), (Boolean) f.defVal());
                    }
                  } else {
                    builder.requiredBoolean(field.name());
                  }
                }
                case TabSchema.UnionS f -> addUnion(builder, f);
                case TabSchema.StructS f ->
                    throw new TabValueException(
                        "StructS is not yet supported as a field of a StructS when reading from an Avro file.",
                        f);
              }
            });
    return builder.endRecord();
  }

  private static SchemaBuilder.FieldAssembler<Schema> addUnion(
      SchemaBuilder.FieldAssembler<Schema> builder, TabSchema.UnionS f) {
    SchemaBuilder.UnionAccumulator<SchemaBuilder.NullDefault<Schema>> b =
        builder.name(f.name()).type().unionOf().nullType();
    for (Class<?> c : f.types()) {
      b =
          switch (c.getName()) {
            case "java.lang.Integer" -> b.and().intType();
            case "java.lang.Long" -> b.and().longType();
            case "java.lang.Float" -> b.and().floatType();
            case "java.lang.Double" -> b.and().doubleType();
            case "java.lang.Boolean" -> b.and().booleanType();
            case "java.lang.String" -> b.and().stringType();
            default ->
                throw new IllegalArgumentException(
                    "Cannot handle this type in tabular data: " + c.getName());
          };
    }
    return b.endUnion().nullDefault();
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

  private record ReaderTabStream(TabSchema.StructS schema, Stream<TabRecord> stream)
      implements TabStream {

    public List<String> fieldNames() {
      return schema().fields().stream().map(TabSchema::name).toList();
    }

    @Override
    public void close() throws IOException {
      stream().close();
    }
  }
}
