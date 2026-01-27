package com.redsaz.lognition.convert;

import static com.redsaz.lognition.convert.ConverterBaseTest.assertContentEquals;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import org.testng.annotations.Test;

public class AvrosTest {

  @Test
  public void testReadWriteSimple() throws IOException {
    // Given a CSV file with no null/empty values with strings, ints, longs, floats, doubles, and
    // booleans,
    String content =
        """
        exampleLong,exampleInt,exampleString,exampleFloat,exampleDouble,exampleBoolean
        1766362285195,104,GET /logs/test,1.5,3.25,true
        1766362285191,111,PUT /logs/test,2.125,6.0625,false
        """;

    // and a schema that accounts for every column,
    String schemaStr =
        """
        {"namespace": "com.redsaz.lognition",
        "type": "record",
        "name": "ExampleRecord",
        "fields": [
            {"name": "exampleLong", "type": "long"},
            {"name": "exampleInt", "type": "int"},
            {"name": "exampleString", "type": "string"},
            {"name": "exampleFloat", "type": "float"},
            {"name": "exampleDouble", "type": "double"},
            {"name": "exampleBoolean", "type": "boolean"}
        ]
        }
        """;
    TabSchema.StructS schema = TabSchema.StructS.of(schemaStr);

    // When it is loaded into tabular data with the schema,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream csv = Csvs.records(sourceFile.path(), schema);
        TempContent destAvroFile = TempContent.withName("converted", ".avro");
        TempContent destCsvFile = TempContent.withName("exported", ".csv")) {

      // and written to an avro file,
      Avros.write(destAvroFile.path(), csv.schema(), csv.stream());

      // and read from the avro file,
      try (TabStream avro = Avros.records(destAvroFile.path())) {
        // and written back into a CSV file,
        Csvs.write(destCsvFile.path(), avro.schema(), avro.stream());
      }

      // Then the source CSV file and the result CSV file contents are functionally the same.
      assertContentEquals(destCsvFile.content(), content, "Reconstituted CSV data");

      // TODO: I think Tabulars et al should be in core or API (probably api) rather than convert,
      // perhaps.
      // Csvs should stay in convert, but get rid of CsvStream. Instead, Csvs should output a
      // TabStream.
    }
  }

  @Test
  public void testReadWriteUnion() throws IOException {
    // Given a CSV file with no null/empty values with various union combos,
    String content =
        """
        exampleLongString,exampleIntString,exampleFloatString,exampleDoubleString,exampleBooleanString
        strval1,2,3.5,4.25,true
        1,strval2,4.5,5.25,false
        1,3,strval3,6.25,true
        1,4,5.5,strval4,false
        1,4,5.5,7.25,strval5
        """;

    // and a schema that accounts for every column,
    TabSchema.StructS schema =
        TabSchema.StructS.of(
            "exampleRecord",
            TabSchema.UnionS.optional("exampleLongString", Long.class, String.class),
            TabSchema.UnionS.required("exampleIntString", Integer.class, String.class),
            TabSchema.UnionS.required("exampleFloatString", Float.class, String.class),
            TabSchema.UnionS.required("exampleDoubleString", Double.class, String.class),
            TabSchema.UnionS.required("exampleBooleanString", Boolean.class, String.class));

    // When it is loaded as tabular data with the schema,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream csv = Csvs.records(sourceFile.path(), schema);
        TempContent destAvroFile = TempContent.withName("converted", ".avro");
        TempContent destCsvFile = TempContent.withName("exported", ".csv")) {

      // and written to an avro file,
      Avros.write(destAvroFile.path(), csv.schema(), csv.stream());

      // and read from the avro file,
      try (TabStream avro = Avros.records(destAvroFile.path())) {
        List<TabRecord> actualRows = avro.stream().toList();
        List<TabRecord> expectedRows =
            List.of(
                TabRecord.of("strval1", 2, 3.5f, 4.25d, true),
                TabRecord.of(1L, "strval2", 4.5f, 5.25d, false),
                TabRecord.of(1L, 3, "strval3", 6.25d, true),
                TabRecord.of(1L, 4, 5.5f, "strval4", false),
                TabRecord.of(1L, 4, 5.5f, 7.25d, "strval5"));
        // Then the results have the expected values.
        assertEquals(actualRows, expectedRows);
      }
    }
  }

  @Test
  public void testReadWriteJtl() throws IOException {
    // This JTL data was (mostly) taken from a real 10-thread jmeter run.
    String content =
        """
          timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect
          1766362285195,104,GET /logs/test,200,OK,Thread Group 1-1,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key2=val2,104,0,1
          1766362285191,111,PUT /logs/test,200,OK,Thread Group 1-5,text,true,,538,287,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key0=val0,111,0,1
          1766362285205,101,GET /logs/test,200,OK,Thread Group 1-10,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key6=val6,101,0,0
          1766362285195,112,GET /logs/test,200,OK,Thread Group 1-3,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key1=val1,111,0,3
          1766362285197,112,GET /logs/test,200,OK,Thread Group 1-4,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key3=val3,112,0,1
          1766362285202,108,GET /logs/test,200,OK,Thread Group 1-9,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key5=val5,108,0,1
          1766362285197,115,GET /logs/test,200,OK,Thread Group 1-2,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key4=val4,115,0,1
          1766362285284,103,GET /logs/test,200,OK,Thread Group 1-6,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key7=val7,103,0,0
          1766362285292,101,GET /logs/test,400,Bad Request,Thread Group 1-7,text,false,,502,242,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key8=val8&status=400,100,0,0
          1766362285297,102,POST /logs/test,200,OK,Thread Group 1-8,text,true,,539,288,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key9=val9,102,0,0
          1766362285303,97,PUT /logs/test,200,OK,Thread Group 1-5,text,true,,547,296,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key11=val11,97,0,1
          1766362285308,98,GET /logs/test,500,Internal Server Error,Thread Group 1-10,text,false,,514,244,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key12=val12&status=500,98,0,0
          1766362285300,110,GET /logs/test,404,Not Found,Thread Group 1-1,text,false,,502,244,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key10=val10&status=404,110,0,1
          1766362285310,1,GET /logs/test,Non HTTP response code: org.apache.http.conn.HttpHostConnectException,Non HTTP response message: Connect to 127.0.0.1:8080 [/127.0.0.1] failed: Connection refused,Thread Group 1-4,text,false,,2546,0,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key1=val1,0,0,1
          """;

    TabSchema.StructS schema =
        TabSchema.StructS.of(
            "exampleRecord",
            TabSchema.LongS.required("timeStamp"),
            TabSchema.LongS.required("elapsed"),
            TabSchema.StrS.optional("label", ""),
            TabSchema.UnionS.optional("responseCode", Integer.class, String.class),
            TabSchema.StrS.optional("responseMessage"),
            TabSchema.StrS.optional("threadName"),
            TabSchema.StrS.optional("dataType"),
            TabSchema.BooleanS.optional("success"),
            TabSchema.StrS.optional("failureMessage"),
            TabSchema.LongS.optional("bytes"),
            TabSchema.LongS.optional("sentBytes"),
            TabSchema.IntS.optional("grpThreads"),
            TabSchema.IntS.optional("allThreads"),
            TabSchema.StrS.optional("URL"),
            TabSchema.IntS.optional("Latency"),
            TabSchema.IntS.optional("IdleTime"),
            TabSchema.IntS.optional("Connect"));

    // Given a JMeter JTL CSV file,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream csv = Csvs.records(sourceFile.path(), schema);
        TempContent destAvroFile = TempContent.withName("converted", ".avro");
        TempContent destCsvFile = TempContent.withName("exported", ".csv")) {
      // When it is converted into an avro file,
      Avros.write(destAvroFile.path(), schema, csv.stream());

      // and read from the avro file as a TabStream,
      try (TabStream exporting = Avros.records(destAvroFile.path())) {
        // and is exported back into a CSV file,
        Csvs.write(destCsvFile.path(), exporting.schema(), exporting.stream());
      }

      // Then the source CSV file and the result CSV file contents are functionally the same.
      assertContentEquals(destCsvFile.content(), content, "Reconstituted CSV data");
      //      // ...But we still haven't gotten it hooked into the legacy Sample world, yet.
      //      // I don't think this should be done at this layer. It should be a transforming layer.
      //
      //      // Sample schema looks like it's re-implementing an incomplete grafana/Prometheus,
      // poorly
      //      StructS sampleSchema = StructS.of(
      //          TabSchema.LongS.required("timeStamp"),
      //          TabSchema.LongS.required("elapsed"),
      //          TabSchema.StrS.optional("label"),
      //          TabSchema.UnionS.optional("responseCode", Integer.class, String.class),
      //          TabSchema.LongS.required("bytes")
      //      );
    }
  }
}
