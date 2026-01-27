package com.redsaz.lognition.convert;

import static com.redsaz.lognition.convert.ConverterBaseTest.assertContentEquals;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

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
        TabStream records = Csvs.recordsAsStrings(sourceFile.path())) {

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
    TabField.StructF schema = TabField.StructF.of(schemaStr);

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
    TabField.StructF schema = TabField.StructF.of(schemaStr);

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
    TabField.StructF schema = TabField.StructF.of(schemaStr);

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
    TabField.StructF schema =
        TabField.StructF.of(
            "exampleRecord",
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
    TabField.StructF schema =
        TabField.StructF.of(
            "exampleRecord",
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

  @Test
  public void testReadMissingOptionalColumnWithSchema() throws IOException {
    // Given a schema that has optional fields, both with and without default values,
    TabField.StructF schema =
        TabField.StructF.of(
            "exampleRecord",
            TabField.LongF.required("exampleLong"),
            TabField.LongF.optional("exampleMissingLongDef", -1L),
            TabField.LongF.optional("exampleMissingLong"),
            TabField.IntF.required("exampleInt"),
            TabField.IntF.optional("exampleMissingIntDef", -1),
            TabField.IntF.optional("exampleMissingInt"),
            TabField.StrF.required("exampleString"),
            TabField.StrF.optional("exampleMissingStringDef", "missing"),
            TabField.StrF.optional("exampleMissingString"),
            TabField.FloatF.required("exampleFloat"),
            TabField.FloatF.optional("exampleMissingFloatDef", -1.0F),
            TabField.FloatF.optional("exampleMissingFloat"),
            TabField.DoubleF.required("exampleDouble"),
            TabField.DoubleF.optional("exampleMissingDoubleDef", -2.0d),
            TabField.DoubleF.optional("exampleMissingDouble"),
            TabField.BooleanF.required("exampleBoolean"),
            TabField.BooleanF.optional("exampleMissingBooleanDef", true),
            TabField.BooleanF.optional("exampleMissingBoolean"));

    // and a CSV that does not have columns for those optional fields,
    String content =
        """
        exampleLong,exampleInt,exampleString,exampleFloat,exampleDouble,exampleBoolean
        1766362285195,104,GET /logs/test,1.5,3.25,true
        1766362285191,111,PUT /logs/test,2.125,6.0625,false
        """;

    // When it is loaded as tabular data with the schema,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records = Csvs.records(sourceFile.path(), schema)) {
      // Then the CSV schema should be identical to the given schema,
      assertSame(records.schema(), schema);

      // and each record should have the expected values in the types specified in the schema,
      // including from the default values for the optionals.
      List<TabRecord> actualRows = records.stream().toList();
      List<TabRecord> expectedRows =
          List.of(
              TabRecord.of(
                  1766362285195L,
                  -1L,
                  null,
                  104,
                  -1,
                  null,
                  "GET /logs/test",
                  "missing",
                  null,
                  1.5f,
                  -1F,
                  null,
                  3.25d,
                  -2D,
                  null,
                  true,
                  true,
                  null),
              TabRecord.of(
                  1766362285191L,
                  -1L,
                  null,
                  111,
                  -1,
                  null,
                  "PUT /logs/test",
                  "missing",
                  null,
                  2.125F,
                  -1F,
                  null,
                  6.0625d,
                  -2D,
                  null,
                  false,
                  true,
                  null));
      assertEquals(actualRows, expectedRows);
    }
  }

  @Test(dataProvider = "requireds", expectedExceptions = TabValueRequiredException.class)
  public void testReadWriteRequired(String content, String message) throws IOException {
    // Given a CSV file with null values,
    try (TempContent sourceFile = TempContent.of(content);
        TempContent destCsvFile = TempContent.withName("converted", ".csv")) {

      // and a schema that requires a value instead of null,
      TabField.StructF schema =
          TabField.StructF.of(
              "exampleRecord",
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
      }
      // Then an exception should be thrown due to failed constraint.
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
    TabField.StructF schema =
        TabField.StructF.of(
            "exampleRecord",
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
    TabField.StructF schema =
        TabField.StructF.of(
            "exampleRecord",
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
    TabField.StructF schema =
        TabField.StructF.of(
            "exampleRecord",
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
        TabStream records = Csvs.recordsAsStrings(sourceFile.path())) {

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
        TabStream records = Csvs.recordsAsStrings(sourceFile.path())) {

      // and written back out into a CSV,
      Csvs.write(destFile.path(), records.schema(), records.stream());

      // Then the source CSV file and the result CSV file contents should be empty.
      assertContentEquals(destFile.content(), content, "Reconstituted CSV data");
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSchemaBadDuplicateNames() {
    // TODO: This should be in a TabSchemaTest
    // When a schema is specified with two fields with the same name,
    TabField.StructF.of(
        "exampleRecord", TabField.IntF.required("name"), TabField.IntF.required("name"));
    // Then an exception is thrown explaining that duplicate names were detected.
  }

  @Test
  public void testReadWriteCsvWithExtraAdded() throws IOException {
    // Given a CSV file that has a column not covered by the schema,
    String content =
        """
        exampleLong,exampleExtra
        1766362285195,example1
        1766362285191,example2
        """;

    TabField.StructF schema =
        TabField.StructF.of("exampleRecord", TabField.LongF.required("exampleLong"));

    // When the data is read and configured to allow extra columns,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records =
            Csvs.records(sourceFile.path(), schema, Csvs.SimpleReadOption.ADD_UNKNOWN)) {

      // Then the resulting schema includes the extra column as a string,
      // and the records include the values from the column
      List<TabRecord> actualRows = records.stream().toList();
      List<TabRecord> expectedRows =
          List.of(
              TabRecord.of(1766362285195L, "example1"), TabRecord.of(1766362285191L, "example2"));
      assertEquals(actualRows, expectedRows);
    }
  }

  @Test
  public void testReadWriteCsvWithExtraIgnored() throws IOException {
    // Given a CSV file that has a column not covered by the schema,
    String content =
        """
        exampleLong,exampleExtra
        1766362285195,example1
        1766362285191,example2
        """;

    TabField.StructF schema =
        TabField.StructF.of("exampleRecord", TabField.LongF.required("exampleLong"));

    // When the data is read and configured to ignore extra columns (it is the default),
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records = Csvs.records(sourceFile.path(), schema)) {

      // Then the resulting schema includes the extra column as a string,
      // and the records include the values from the column
      List<TabRecord> actualRows = records.stream().toList();
      List<TabRecord> expectedRows =
          List.of(TabRecord.of(1766362285195L), TabRecord.of(1766362285191L));
      assertEquals(actualRows, expectedRows);
    }
  }

  @Test
  public void testReadWriteCsvWithNonIntValueIgnore() throws IOException {
    // Given a CSV file that has a value that is normally an int but is instead a string,
    // and a schema with an optional int,
    String content =
        """
        exampleLong,exampleMistyped
        1766362285195,10
        1766362285191,example2
        """;

    TabField.StructF schema =
        TabField.StructF.of(
            "exampleRecord",
            TabField.LongF.required("exampleLong"),
            TabField.IntF.optional("exampleMistyped"));

    // When the value is read and configured to ignore mistyped info,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records =
            Csvs.records(sourceFile.path(), schema, Csvs.SimpleReadOption.IGNORE_MISTYPED)) {

      // Then any records with the value mistyped is included, but the mistyped value is null.
      List<TabRecord> actualRows = records.stream().toList();
      List<TabRecord> expectedRows =
          List.of(TabRecord.of(1766362285195L, 10), TabRecord.of(1766362285191L, null));
      assertEquals(actualRows, expectedRows);
    }
  }

  @Test(expectedExceptions = TabValueMistypedException.class)
  public void testReadWriteCsvWithNonIntValueIgnoreFails() throws IOException {
    // Given a CSV file that has a value that is normally an int but is instead a string,
    // and a schema with a required int,
    String content =
        """
        exampleLong,exampleMistyped
        1766362285195,10
        1766362285191,example2
        """;

    TabField.StructF schema =
        TabField.StructF.of(
            "exampleRecord",
            TabField.LongF.required("exampleLong"),
            TabField.IntF.required("exampleMistyped"));

    // When the value is read and configured to ignore mistyped info,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records =
            Csvs.records(sourceFile.path(), schema, Csvs.SimpleReadOption.IGNORE_MISTYPED)) {
      // Then an exception is thrown because the value is required.
      List<TabRecord> actualRows = records.stream().toList();
    }
  }

  @Test
  public void testReadWriteCsvWithErrorHandlerRecover() throws IOException {
    // Given a CSV file that has at least two records, one with a mistyped value and one with a
    // missing value,
    String content =
        """
        exampleLong,exampleInt
        1766362285195,
        1766362285191,bogus
        ,10
        bogus,11
        """;

    // and a schema with a required long and a required int
    TabField.StructF schema =
        TabField.StructF.of(
            "exampleRecord",
            TabField.LongF.required("exampleLong"),
            TabField.IntF.required("exampleInt"));

    // When the value is read and configured to use a custom error handler,
    Function<TabValueException, Csvs.ErrorAction<?>> handler =
        ex -> {
          // This simple error handler will use its own default values when encountering the
          // error.
          // (Why do this instead of specifying a field as optional with a default value? Unsure.)
          return switch (ex) {
            case TabValueRequiredException i -> {
              if (i.schema().type() == Integer.class) {
                yield Csvs.ErrorAction.recover(1);
              } else if (i.schema().type() == Long.class) {
                yield Csvs.ErrorAction.recover(2L);
              }
              yield Csvs.ErrorAction.fail();
            }
            case TabValueMistypedException i -> {
              if (i.schema().type() == Integer.class) {
                yield Csvs.ErrorAction.recover(3);
              } else if (i.schema().type() == Long.class) {
                yield Csvs.ErrorAction.recover(4L);
              }
              yield Csvs.ErrorAction.fail();
            }
            case TabValueException i -> {
              throw i;
            }
          };
        };
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records = Csvs.records(sourceFile.path(), schema, Csvs.handleErrors(handler))) {
      // Then the records are created and using the "recovered" values
      List<TabRecord> actualRows = records.stream().toList();
      List<TabRecord> expectedRows =
          List.of(
              TabRecord.of(1766362285195L, 1),
              TabRecord.of(1766362285191L, 3),
              TabRecord.of(2L, 10),
              TabRecord.of(4L, 11));
      assertEquals(actualRows, expectedRows);
    }
  }

  @Test(expectedExceptions = TabValueException.class)
  public void testReadWriteCsvWithBadErrorHandlerRecover() throws IOException {
    // Given a CSV file that has at least two records, one with a mistyped value and one with a
    // missing value,
    String content =
        """
        exampleLong,exampleInt
        1766362285195,
        1766362285191,bogus
        ,10
        bogus,11
        """;

    // and a schema with a required long and a required int
    TabField.StructF schema =
        TabField.StructF.of(
            "exampleRecord",
            TabField.LongF.required("exampleLong"),
            TabField.IntF.required("exampleInt"));

    // When the value is read and configured to use a custom error handler that erroneously returns
    // either a null or mistyped value for the "recovered" value,
    Function<TabValueException, Csvs.ErrorAction<?>> handler =
        ex -> {
          // This simple error handler will use its own default values when encountering the
          // error.
          // (Why do this instead of specifying a field as optional with a default value? Unsure.)
          return switch (ex) {
            case TabValueRequiredException i -> Csvs.ErrorAction.recover(null);
            case TabValueMistypedException i -> Csvs.ErrorAction.recover("Still bad");
            case TabValueException i -> throw new RuntimeException("Fail this.");
          };
        };
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records = Csvs.records(sourceFile.path(), schema, Csvs.handleErrors(handler))) {
      // Then an exception is thrown because the recovery is still bad.
      List<TabRecord> actualRows = records.stream().toList();
    }
  }

  @Test
  public void testReadWriteCsvWithErrorHandlerSkip() throws IOException {
    // Given a CSV file that has at least three records, one with a mistyped value, one with a
    // missing value, and one with a good value
    String content =
        """
        exampleLong,exampleInt
        1766362285195,
        1766362285191,bogus
        1766362285205,10
        """;

    // and a schema with a required long and a required int
    TabField.StructF schema =
        TabField.StructF.of(
            "exampleRecord",
            TabField.LongF.required("exampleLong"),
            TabField.IntF.required("exampleInt"));

    // When the value is read and configured to use a custom error handler that skips records,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records =
            Csvs.records(
                sourceFile.path(), schema, Csvs.handleErrors(e -> Csvs.ErrorAction.skip()))) {
      // Then the records with bad values are skipped and good values are included.
      List<TabRecord> actualRows = records.stream().toList();
      List<TabRecord> expectedRows = List.of(TabRecord.of(1766362285205L, 10));
      assertEquals(actualRows, expectedRows);
    }
  }

  @Test(expectedExceptions = TabValueRequiredException.class)
  public void testReadWriteCsvWithErrorHandlerFail() throws IOException {
    // Given a CSV file that has at least three records, one with a mistyped value, one with a
    // missing value, and one with a good value
    String content =
        """
        exampleLong,exampleInt
        1766362285195,
        1766362285191,bogus
        1766362285205,10
        """;

    // and a schema with a required long and a required int
    TabField.StructF schema =
        TabField.StructF.of(
            "exampleRecord",
            TabField.LongF.required("exampleLong"),
            TabField.IntF.required("exampleInt"));

    // When the value is read and configured to use a custom error handler that fails the operation,
    try (TempContent sourceFile = TempContent.of(content);
        TabStream records =
            Csvs.records(
                sourceFile.path(), schema, Csvs.handleErrors(e -> Csvs.ErrorAction.fail()))) {
      // Then the operation fails when the first bad value is hit.
      List<TabRecord> actualRows = records.stream().toList();
    }
  }
}
