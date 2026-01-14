package com.redsaz.lognition.convert;

import static com.redsaz.lognition.convert.ConverterBaseTest.assertContentEquals;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.List;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CsvsTest {

  @Test
  public void testReadWriteSchemaless() throws IOException {
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

    // Given a normal, everyday CSV file (not necessarily a Jmeter JTL or Loady log file),
    // When the source CSV is read in and written back out without a provided schema,
    try (TempContent sourceFile = TempContent.of(content);
        TempContent destFile = TempContent.withName("converted", ".csv");
        TabStream records = Csvs.records(sourceFile.path())) {

      // and written back out into a CSV,
      Csvs.write(destFile.path(), records.schema(), records.stream());

      // Then the source CSV file and the result CSV file contents are functionally the same.
      assertContentEquals(destFile.content(), content, "Reconstituted CSV data");
    }
  }

  @Test
  public void testReadWriteSimpleWithSchema() throws IOException {
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
        TempContent destCsvFile = TempContent.withName("converted", ".csv")) {

      // When it is loaded as tabular data with the schema,
      try (TabStream records = Csvs.records(sourceFile.path(), schema)) {
        // and written back into a CSV file,
        Csvs.write(destCsvFile.path(), records.schema(), records.stream());
      }

      // Then the source CSV file and the result CSV file contents are functionally the same.
      assertContentEquals(destCsvFile.content(), content, "Reconstituted CSV data");
    }
  }

  @Test
  public void testReadSimpleWithSchema() throws IOException {
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

    // When it is loaded as tabular data with the schema,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records = Csvs.records(sourceFile.path(), schema)) {
      // Then the CSV schema should be identical to the given schema,
      assertSame(records.schema(), schema);

      // and each record should have the expected values in the types specified in the schema
      List<TabRecord> actualRows = records.stream().toList();
      List<TabRecord> expectedRows =
          List.of(
              TabRecord.of(1766362285195L, 104, "GET /logs/test", 1.5f, 3.25d, true),
              TabRecord.of(1766362285191L, 111, "PUT /logs/test", 2.125f, 6.0625d, false));
      assertEquals(actualRows, expectedRows);
    }
  }

  @Test
  public void testReadSimpleWithSchemaDifferentOrder() throws IOException {
    // Given a CSV file with no null/empty values with strings, ints, longs, floats, doubles, and
    // booleans,
    String content =
        """
        exampleLong,exampleInt,exampleString,exampleFloat,exampleDouble,exampleBoolean
        1766362285195,104,GET /logs/test,1.5,3.25,true
        1766362285191,111,PUT /logs/test,2.125,6.0625,false
        """;

    // and a schema that accounts for every column but is a different order than what appears in the
    // CSV,
    String schemaStr =
        """
        {"namespace": "com.redsaz.lognition",
        "type": "record",
        "name": "ExampleRecord",
        "fields": [
            {"name": "exampleBoolean", "type": "boolean"},
            {"name": "exampleDouble", "type": "double"},
            {"name": "exampleFloat", "type": "float"},
            {"name": "exampleString", "type": "string"},
            {"name": "exampleInt", "type": "int"},
            {"name": "exampleLong", "type": "long"}
        ]
        }
        """;
    TabSchema schema = TabSchema.of(schemaStr);

    // When it is loaded as tabular data with the schema,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records = Csvs.records(sourceFile.path(), schema)) {
      // Then the CSV schema should be identical to the given schema,
      assertSame(records.schema(), schema);

      // and each record should have the expected values in the types specified in the schema
      // and the values are in the order specified by the schema.
      List<TabRecord> actualRows = records.stream().toList();
      List<TabRecord> expectedRows =
          List.of(
              TabRecord.of(true, 3.25d, 1.5f, "GET /logs/test", 104, 1766362285195L),
              TabRecord.of(false, 6.0625d, 2.125f, "PUT /logs/test", 111, 1766362285191L));
      assertEquals(actualRows, expectedRows);
    }
  }

  @Test
  public void testReadWriteWithNulls() throws IOException {
    // Given a CSV file with null values,
    String content =
        """
        exampleLong,exampleInt,exampleString,exampleFloat,exampleDouble,exampleBoolean
        ,104,GET /logs/test,1.5,3.25,true
        1766362285191,,PUT /logs/test,2.125,6.0625,false
        1766362285205,101,,3.03125,4.5,true
        1766362285195,112,GET /logs/test,,6.0625,false
        1766362285197,112,GET /logs/test,7.25,,true
        1766362285202,108,GET /logs/test,9.0625,10.03125,
        """;

    // and a schema that allows for nulls for the columns with nulls,
    TabSchema schema =
        TabSchema.of(
            TabField.LongF.optional("exampleLong"),
            TabField.IntF.optional("exampleInt"),
            TabField.StrF.optional("exampleString"),
            TabField.FloatF.optional("exampleFloat"),
            TabField.DoubleF.optional("exampleDouble"),
            TabField.BooleanF.optional("exampleBoolean"));

    try (TempContent sourceFile = TempContent.of(content);
        TempContent destCsvFile = TempContent.withName("converted", ".csv")) {

      // When it is loaded as tabular data with the schema,
      try (TabStream records = Csvs.records(sourceFile.path(), schema)) {
        List<TabRecord> actualRows = records.stream().toList();

        // and written back into a CSV file,
        Csvs.write(destCsvFile.path(), records.schema(), actualRows.stream());

        // Then the data should be null when not provided in the source,
        List<TabRecord> expectedRows =
            List.of(
                TabRecord.of(null, 104, "GET /logs/test", 1.5f, 3.25d, true),
                TabRecord.of(1766362285191L, null, "PUT /logs/test", 2.125f, 6.0625d, false),
                TabRecord.of(1766362285205L, 101, null, 3.03125f, 4.5d, true),
                TabRecord.of(1766362285195L, 112, "GET /logs/test", null, 6.0625d, false),
                TabRecord.of(1766362285197L, 112, "GET /logs/test", 7.25f, null, true),
                TabRecord.of(1766362285202L, 108, "GET /logs/test", 9.0625f, 10.03125d, null));
        assertEquals(actualRows, expectedRows);
      }

      // and the source CSV file and the result CSV file contents are functionally the same.
      assertContentEquals(destCsvFile.content(), content, "Reconstituted CSV data");
    }
  }

  @Test
  public void testReadWriteWithDefaults() throws IOException {
    // Given a CSV file with null values,
    String content =
        """
        exampleLong,exampleInt,exampleString,exampleFloat,exampleDouble,exampleBoolean
        ,104,GET /logs/test,1.5,3.25,true
        1766362285191,,PUT /logs/test,2.125,6.0625,false
        1766362285205,101,,3.03125,4.5,true
        1766362285195,112,GET /logs/test,,6.0625,false
        1766362285197,112,GET /logs/test,7.25,,true
        1766362285202,108,GET /logs/test,9.0625,10.03125,
        """;

    // and a schema that allows for default values for the columns with nulls,
    TabSchema schema =
        TabSchema.of(
            TabField.LongF.optional("exampleLong", 1L),
            TabField.IntF.optional("exampleInt", 2),
            TabField.StrF.optional("exampleString", "value"),
            TabField.FloatF.optional("exampleFloat", 3.5f),
            TabField.DoubleF.optional("exampleDouble", 4.25d),
            TabField.BooleanF.optional("exampleBoolean", true));

    try (TempContent sourceFile = TempContent.of(content);
        TempContent destCsvFile = TempContent.withName("converted", ".csv")) {

      // When it is loaded as tabular data with the schema,
      try (TabStream records = Csvs.records(sourceFile.path(), schema)) {
        List<TabRecord> actualRows = records.stream().toList();

        // and written back into a CSV file,
        Csvs.write(destCsvFile.path(), records.schema(), actualRows.stream());

        // Then the data should be null when not provided in the source,
        List<TabRecord> expectedRows =
            List.of(
                TabRecord.of(1L, 104, "GET /logs/test", 1.5f, 3.25d, true),
                TabRecord.of(1766362285191L, 2, "PUT /logs/test", 2.125f, 6.0625d, false),
                TabRecord.of(1766362285205L, 101, "value", 3.03125f, 4.5d, true),
                TabRecord.of(1766362285195L, 112, "GET /logs/test", 3.5f, 6.0625d, false),
                TabRecord.of(1766362285197L, 112, "GET /logs/test", 7.25f, 4.25d, true),
                TabRecord.of(1766362285202L, 108, "GET /logs/test", 9.0625f, 10.03125d, true));
        assertEquals(actualRows, expectedRows);
      }

      // and the resulting CSV file is written with the default values in place of the nulls, all
      // other values are the same.
      String expectedContent =
          """
          exampleLong,exampleInt,exampleString,exampleFloat,exampleDouble,exampleBoolean
          1,104,GET /logs/test,1.5,3.25,true
          1766362285191,2,PUT /logs/test,2.125,6.0625,false
          1766362285205,101,value,3.03125,4.5,true
          1766362285195,112,GET /logs/test,3.5,6.0625,false
          1766362285197,112,GET /logs/test,7.25,4.25,true
          1766362285202,108,GET /logs/test,9.0625,10.03125,true
          """;

      assertContentEquals(destCsvFile.content(), expectedContent, "Reconstituted CSV data");
    }
  }

  @Test(dataProvider = "requireds")
  public void testReadWriteRequired(String content, String message) throws IOException {
    // Given a CSV file with null values,
    try (TempContent sourceFile = TempContent.of(content);
        TempContent destCsvFile = TempContent.withName("converted", ".csv")) {

      // and a schema that requires a value instead of null,
      TabSchema schema =
          TabSchema.of(
              TabField.LongF.required("exampleLong"),
              TabField.IntF.required("exampleInt"),
              TabField.StrF.required("exampleString"),
              TabField.FloatF.required("exampleFloat"),
              TabField.DoubleF.required("exampleDouble"),
              TabField.BooleanF.required("exampleBoolean"));

      // When it is loaded as tabular data with the schema and read,
      try (TabStream records = Csvs.records(sourceFile.path(), schema)) {
        records.stream().toList();
        fail("Should fail due to encountering a null value with: " + message);
      } catch (IllegalArgumentException ex) {
        // Then an exception should be thrown due to failed constraint.
      }
    }
  }

  @DataProvider(name = "requireds")
  public Object[][] requireds() {
    return new Object[][] {
      {
        """
        exampleLong,str
        1,a
        ,b
        """,
        "Required: long"
      },
      {
        """
        exampleInt,str
        1,a
        ,b
        """,
        "Required: int"
      },
      {
        """
        exampleString,str
        value,a
        ,b
        """,
        "Required: string"
      },
      {
        """
        exampleFloat,str
        1.5,a
        ,b
        """,
        "Required: float"
      },
      {
        """
        exampleDouble,str
        1.5,a
        ,b
        """,
        "Required: double"
      },
      {
        """
        exampleBoolean,str
        true,a
        false,b
        ,c
        """,
        "Required: boolean"
      }
    };
  }

  @Test
  public void testReadUnionWithSchema() throws IOException {
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
    TabSchema schema =
        TabSchema.of(
            TabField.UnionF.required("exampleLongString", Long.class, String.class),
            TabField.UnionF.required("exampleIntString", Integer.class, String.class),
            TabField.UnionF.required("exampleFloatString", Float.class, String.class),
            TabField.UnionF.required("exampleDoubleString", Double.class, String.class),
            TabField.UnionF.required("exampleBooleanString", Boolean.class, String.class));

    // When it is loaded as tabular data with the schema,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records = Csvs.records(sourceFile.path(), schema)) {
      // Then the CSV schema should be identical to the given schema,
      assertSame(records.schema(), schema);

      // and each record should have the expected values in the types specified in the schema
      List<TabRecord> actualRows = records.stream().toList();
      List<TabRecord> expectedRows =
          List.of(
              TabRecord.of("strval1", 2, 3.5f, 4.25d, true),
              TabRecord.of(1L, "strval2", 4.5f, 5.25d, false),
              TabRecord.of(1L, 3, "strval3", 6.25d, true),
              TabRecord.of(1L, 4, 5.5f, "strval4", false),
              TabRecord.of(1L, 4, 5.5f, 7.25d, "strval5"));
      assertEquals(actualRows, expectedRows);
    }
  }

  @Test
  public void testReadUnionNullsWithSchema() throws IOException {
    // Given a CSV file with no null/empty values with various union combos,
    String content =
        """
        exampleLongString,exampleIntString,exampleFloatString,exampleDoubleString,exampleBooleanString
        strval1,2,3.5,4.25,
        ,strval2,4.5,5.25,false
        1,,strval3,6.25,true
        1,4,,strval4,false
        1,4,5.5,,strval5
        """;

    // and a schema that accounts for every column,
    TabSchema schema =
        TabSchema.of(
            TabField.UnionF.optional("exampleLongString", Long.class, String.class),
            TabField.UnionF.optional("exampleIntString", Integer.class, String.class),
            TabField.UnionF.optional("exampleFloatString", Float.class, String.class),
            TabField.UnionF.optional("exampleDoubleString", Double.class, String.class),
            TabField.UnionF.optional("exampleBooleanString", Boolean.class, String.class));

    // When it is loaded as tabular data with the schema,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records = Csvs.records(sourceFile.path(), schema)) {
      // Then the CSV schema should be identical to the given schema,
      assertSame(records.schema(), schema);

      // and each record should have the expected values in the types specified in the schema
      List<TabRecord> actualRows = records.stream().toList();
      List<TabRecord> expectedRows =
          List.of(
              TabRecord.of("strval1", 2, 3.5f, 4.25d, null),
              TabRecord.of(null, "strval2", 4.5f, 5.25d, false),
              TabRecord.of(1L, null, "strval3", 6.25d, true),
              TabRecord.of(1L, 4, null, "strval4", false),
              TabRecord.of(1L, 4, 5.5f, null, "strval5"));
      assertEquals(actualRows, expectedRows);
    }
  }

  @Test
  public void testReadUnionDefaultsWithSchema() throws IOException {
    // Given a CSV file with no null/empty values with various union combos,
    String content =
        """
        exampleLongString,exampleIntString,exampleFloatString,exampleDoubleString,exampleBooleanString
        strval1,2,3.5,4.25,
        ,strval2,4.5,5.25,false
        1,,strval3,6.25,true
        1,4,,strval4,false
        1,4,5.5,,strval5
        """;

    // and a schema that accounts for every column,
    TabSchema schema =
        TabSchema.of(
            TabField.UnionF.optional("exampleLongString", "test-def1", Long.class, String.class),
            TabField.UnionF.optional("exampleIntString", "test-def2", Integer.class, String.class),
            TabField.UnionF.optional("exampleFloatString", "test-def3", Float.class, String.class),
            TabField.UnionF.optional(
                "exampleDoubleString", "test-def4", Double.class, String.class),
            TabField.UnionF.optional(
                "exampleBooleanString", "test-def5", Boolean.class, String.class));

    // When it is loaded as tabular data with the schema,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records = Csvs.records(sourceFile.path(), schema)) {
      // Then the CSV schema should be identical to the given schema,
      assertSame(records.schema(), schema);

      // and each record should have the expected values in the types specified in the schema
      List<TabRecord> actualRows = records.stream().toList();
      List<TabRecord> expectedRows =
          List.of(
              TabRecord.of("strval1", 2, 3.5f, 4.25d, "test-def5"),
              TabRecord.of("test-def1", "strval2", 4.5f, 5.25d, false),
              TabRecord.of(1L, "test-def2", "strval3", 6.25d, true),
              TabRecord.of(1L, 4, "test-def3", "strval4", false),
              TabRecord.of(1L, 4, 5.5f, "test-def4", "strval5"));
      assertEquals(actualRows, expectedRows);
    }
  }

  @Test
  public void testReadWriteEmpty() throws IOException {
    String content = "";

    // Given an empty CSV file,
    // When the source CSV is read in,
    try (TempContent sourceFile = TempContent.of(content);
        TempContent destFile = TempContent.withName("converted", ".csv");
        TabStream records = Csvs.records(sourceFile.path())) {

      // and written back out into a CSV,
      Csvs.write(destFile.path(), records.schema(), records.stream());

      // Then the source CSV file and the result CSV file contents should be empty.
      assertContentEquals(destFile.content(), content, "Reconstituted CSV data");
    }
  }

  @Test
  public void testReadWriteHeaderOnly() throws IOException {
    String content =
        "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect";

    // Given a CSV file with only a header line,
    // When the source CSV is read in,
    try (TempContent sourceFile = TempContent.of(content);
        TempContent destFile = TempContent.withName("converted", ".csv");
        TabStream records = Csvs.records(sourceFile.path())) {

      // and written back out into a CSV,
      Csvs.write(destFile.path(), records.schema(), records.stream());

      // Then the source CSV file and the result CSV file contents should be empty.
      assertContentEquals(destFile.content(), content, "Reconstituted CSV data");
    }
  }
}
