package com.redsaz.lognition.convert;

import static com.redsaz.lognition.convert.ConverterBaseTest.assertContentEquals;

import java.io.IOException;
import org.testng.annotations.Test;

public class TabularsTest {

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
    TabSchema schema = TabSchema.of(schemaStr);

    try (TempContent sourceFile = TempContent.of(content);
        CsvStream csv = Csvs.records(sourceFile.path());
        TempContent destAvroFile = TempContent.withName("converted", ".avro");
        TempContent destCsvFile = TempContent.withName("exported", ".csv")) {

      // When it is converted into tabular data,
      TabStream converted = Tabulars.convert(csv.fieldNames(), csv.stream(), schema);
      Tabulars.write(destAvroFile.path(), converted.schema(), converted.stream());

      // and read from the avro file back into a tabular data item,
      // which is exported back into a CSV file,
      try (TabStream exporting = Tabulars.records(destAvroFile.path())) {
        Csvs.writeRecords(destCsvFile.path(), exporting.fieldNames(), exporting.stream());
      }

      // Then the source CSV file and the result CSV file contents are functionally the same.
      assertContentEquals(destCsvFile.content(), content, "Reconstituted CSV data");

      // TODO: I think there shouldn't be a TabVal and friends. Just normal Object and Integer,
      // Long, etc.
      // Let the TabSchema, TabField, etc be what defines the stuff, no need for a wrapper.

      // TODO: I think Tabulars et al should be in core or API (probably api) rather than convert,
      // perhaps.
      // Csvs should stay in convert, but get rid of CsvStream. Instead, Csvs should output a
      // TabStream.
    }
  }

  //  @Test
  //  public void testReadWriteJtl() throws IOException {
  //    // This JTL data was (mostly) taken from a real 10-thread jmeter run.
  //    String content =
  //        """
  //
  // timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect
  //        1766362285195,104,GET /logs/test,200,OK,Thread Group
  // 1-1,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key2=val2,104,0,1
  //        1766362285191,111,PUT /logs/test,200,OK,Thread Group
  // 1-5,text,true,,538,287,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key0=val0,111,0,1
  //        1766362285205,101,GET /logs/test,200,OK,Thread Group
  // 1-10,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key6=val6,101,0,0
  //        1766362285195,112,GET /logs/test,200,OK,Thread Group
  // 1-3,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key1=val1,111,0,3
  //        1766362285197,112,GET /logs/test,200,OK,Thread Group
  // 1-4,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key3=val3,112,0,1
  //        1766362285202,108,GET /logs/test,200,OK,Thread Group
  // 1-9,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key5=val5,108,0,1
  //        1766362285197,115,GET /logs/test,200,OK,Thread Group
  // 1-2,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key4=val4,115,0,1
  //        1766362285284,103,GET /logs/test,200,OK,Thread Group
  // 1-6,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key7=val7,103,0,0
  //        1766362285292,101,GET /logs/test,400,Bad Request,Thread Group
  // 1-7,text,false,,502,242,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key8=val8&status=400,100,0,0
  //        1766362285297,102,POST /logs/test,200,OK,Thread Group
  // 1-8,text,true,,539,288,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key9=val9,102,0,0
  //        1766362285303,97,PUT /logs/test,200,OK,Thread Group
  // 1-5,text,true,,547,296,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key11=val11,97,0,1
  //        1766362285308,98,GET /logs/test,500,Internal Server Error,Thread Group
  // 1-10,text,false,,514,244,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key12=val12&status=500,98,0,0
  //        1766362285300,110,GET /logs/test,404,Not Found,Thread Group
  // 1-1,text,false,,502,244,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key10=val10&status=404,110,0,1
  //        1766362285310,1,GET /logs/test,Non HTTP response code:
  // org.apache.http.conn.HttpHostConnectException,Non HTTP response message: Connect to
  // 127.0.0.1:8080 [/127.0.0.1] failed: Connection refused,Thread Group
  // 1-4,text,false,,2546,0,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key1=val1,0,0,1
  //        """;
  //
  //    // Given a JMeter JTL CSV file,
  //    try (TempContent sourceFile = TempContent.of(content);
  //        CsvStream csv = Csvs.records(sourceFile.path());
  //        TempContent destAvroFile = TempContent.withName("converted", ".avro");
  //        TempContent destCsvFile = TempContent.withName("exported", ".csv")) {
  //
  //      // When it is converted into an avro file,
  //      String schemaStr =
  //          """
  //          {"namespace": "com.redsaz.lognition",
  //          "type": "record",
  //          "name": "JtlSample",
  //          "fields": [
  //              {"name": "timeStamp", "type": "long"},
  //              {"name": "elapsed", "type": "long"},
  //              {"name": "label", "type": "string", "default": ""},
  //              {"name": "responseCode", "type": ["null", "int", "string"]},
  //              {"name": "responseMessage", "type": ["null", "string"]},
  //              {"name": "threadName", "type": ["null", "string"]},
  //              {"name": "dataType", "type": ["null", "string"]},
  //              {"name": "success", "type": ["null", "boolean"]},
  //              {"name": "failureMessage", "type": ["null", "string"]},
  //              {"name": "bytes", "type": ["null", "long"]},
  //              {"name": "sentBytes", "type": ["null", "long"]},
  //              {"name": "grpThreads", "type": ["null", "int"]},
  //              {"name": "allThreads", "type": ["null", "int"]},
  //              {"name": "URL", "type": ["null", "string"]},
  //              {"name": "Latency", "type": ["null", "int"]},
  //              {"name": "connect", "type": ["null", "int"]},
  //              {"name": "IdleTime", "type": ["null", "int"]}
  //          ]
  //          }
  //          """;
  //
  //      Schema.Parser parser = new Schema.Parser();
  //      // Note: This mostly came from log.avsc. Look at it, it might need updated! We could use
  // it.
  //      Schema schema = parser.parse(schemaStr);
  //
  //      TabSchema tabSchema = TabSchema.of(schemaStr);
  //      TabStream converted = Tabulars.convert(csv.fieldNames(), csv.stream());
  //      Tabulars.write(destAvroFile.path(), converted.schema(), converted.stream());
  //
  //      // and read from the avro file back into a tabular data item,
  //      // which is exported back into a CSV file,
  //      try (TabStream exporting = Tabulars.records(destAvroFile.path())) {
  //        Csvs.writeRecords(destCsvFile.path(), exporting.fieldNames(), exporting.stream());
  //      }
  //
  //      // Then the source CSV file and the result CSV file contents are functionally the same.
  //      assertContentEquals(destCsvFile.content(), content, "Reconstituted CSV data");
  //
  //      // ...But we still haven't gotten it hooked into the legacy Sample world, yet.
  //      // I don't this this should be done at this layer. It should be a transforming layer.
  //
  //      // Sample schema looks like it's re-implementing an incomplete grafana/Prometheus, poorly
  //      String sampleSchema =
  //          """
  //          {
  //            "fields": [
  //              {"name": "timeStamp", "is": "sample.instant(start, ms)"},
  //              {"name": "elapsed", "is": "sample.duration(elapsed, ms)"},
  //              {"name": "label", "is": "sample.label"},
  //              {"name": "responseCode", "is": "sample.status"},
  //              {"name": "bytes", "is": "sample.data(down, bytes)"}
  //            ]
  //          |
  //          """;
  //    }
  //  }
  //
  //  public void testReadWriteJtlWithExtra() {
  //    // Given a JTL file that has a column not covered by the schema,
  //    // When the data is saved to a CSV,
  //    // Then the column was still written to the CSV, assumed to be a string, and the result CSV
  // file
  //    // contents are funtionally the same.
  //    fail("Test not written");
  //  }
  //
  //  public void testReadWriteCsvWithNonIntValue() {
  //    // Given a schema-supplied CSV file that has a value that is normally an int but is instead
  // a
  //    // string,
  //    // When the data is saved to a CSV,
  //    // Then the value is null if the schema supports it, otherwise it is the default.
  //    fail("Test not written");
  //  }
  //
  //  public void testReadWriteCsvWithNonIntValueToFail() {
  //    // Given a schema-supplied CSV file that has a value that is normally an int but is instead
  // a
  //    // string,
  //    // When the data is saved to a CSV,
  //    // Then the value is null if the schema supports it, otherwise it is the default.
  //    fail("Test not written");
  //  }
}
