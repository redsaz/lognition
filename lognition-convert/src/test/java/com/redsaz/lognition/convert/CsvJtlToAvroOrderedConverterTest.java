/*
 * Copyright 2017 Redsaz <redsaz@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redsaz.lognition.convert;

import static org.testng.Assert.assertEquals;

import com.redsaz.lognition.api.exceptions.AppServerException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test The CSV-JTL to Avro converter.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvJtlToAvroOrderedConverterTest extends ConverterBaseTest {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(CsvJtlToAvroOrderedConverterTest.class);

  // This JTL data was (mostly) taken from a real jmeter run.
  private static final String DEFAULT_CONTENT =
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

  @Test
  public void testConvertAndBack() throws IOException {
    // Given JTL-CSV data,
    String content = DEFAULT_CONTENT;

    try (TempContent sourceFile = TempContent.of(content);
        TempContent avroFile = TempContent.withName("converted", ".avro");
        TempContent reconstitutedFile = TempContent.withName("reconstituted", ".jtl")) {

      // When converted into avro format and then converted back into csv format,
      Converter jtl2Avro = new CsvJtlToAvroOrderedConverter();
      String avroHash = jtl2Avro.convert(sourceFile.file(), avroFile.file());

      Converter avro2Jtl = new AvroToCsvJtlConverter();
      String reconstitutedHash = avro2Jtl.convert(avroFile.file(), reconstitutedFile.file());

      // Then the reconstituted csv data should match the original data, but ordered by timestamp
      // and then elapsed, and just these columns:
      // timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,bytes,allThreads
      String expectedStr =
          """
          timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,bytes,allThreads
          1766362285191,111,PUT /logs/test,200,OK,Thread Group 1-5,true,538,10
          1766362285195,104,GET /logs/test,200,OK,Thread Group 1-1,true,468,10
          1766362285195,112,GET /logs/test,200,OK,Thread Group 1-3,true,468,10
          1766362285197,112,GET /logs/test,200,OK,Thread Group 1-4,true,468,10
          1766362285197,115,GET /logs/test,200,OK,Thread Group 1-2,true,468,10
          1766362285202,108,GET /logs/test,200,OK,Thread Group 1-9,true,468,10
          1766362285205,101,GET /logs/test,200,OK,Thread Group 1-10,true,468,10
          1766362285284,103,GET /logs/test,200,OK,Thread Group 1-6,true,468,10
          1766362285292,101,GET /logs/test,400,Bad Request,Thread Group 1-7,false,502,10
          1766362285297,102,POST /logs/test,200,OK,Thread Group 1-8,true,539,10
          1766362285300,110,GET /logs/test,404,Not Found,Thread Group 1-1,false,502,10
          1766362285303,97,PUT /logs/test,200,OK,Thread Group 1-5,true,547,10
          1766362285308,98,GET /logs/test,500,Internal Server Error,Thread Group 1-10,false,514,10
          1766362285310,1,GET /logs/test,Non HTTP response code: org.apache.http.conn.HttpHostConnectException,Non HTTP response message: Connect to 127.0.0.1:8080 [/127.0.0.1] failed: Connection refused,Thread Group 1-4,false,2546,10
          """;
      assertContentEquals(reconstitutedFile.content(), expectedStr, "Reconstituted CSV data");

      // and the hash of the avro-based file should be the (precalculated) expected value.
      assertEquals(
          avroHash, "edf00831268cce0c0c6c2ca08249fb767b047c79e6b26b996f7b3b67230dcf10", "hash");

      // and the hash of the resulting jtl-based file should be the (precalculated) expected value.
      assertEquals(
          reconstitutedHash,
          "d0423e9aae3856cbb91cde79d305536c052672d7fe94dd5087d11919d6402fcf",
          "hash");
    }
  }

  @Test
  public void testConvertConsistent() throws IOException {
    // Tests that two invocations of the converter on the same input data
    // results in the same output. By default the avro writer will add random data headers
    // for reasons we don't need, (we never write to the file more than once.)

    // Given JTL-CSV data,
    String content = DEFAULT_CONTENT;

    try (TempContent sourceFile = TempContent.of(content);
        TempContent avroFile1 = TempContent.withName("converted1", ".avro");
        TempContent avroFile2 = TempContent.withName("converted2", ".avro")) {

      // When converted into avro format two different times,
      Converter jtl2Avro = new CsvJtlToAvroOrderedConverter();
      String avroHash1 = jtl2Avro.convert(sourceFile.file(), avroFile1.file());
      String avroHash2 = jtl2Avro.convert(sourceFile.file(), avroFile2.file());

      // Then the two files should be identical.
      assertEquals(avroHash1, avroHash2, "avro files should be identical content.");
    }
  }

  @Test
  public void testConvertOrdered() throws IOException {
    // Tests that the converter will sort the unordered data, first by timestamp,
    // then by duration, then by label, then by threadname, then by bytes, then by status code,
    // then by status message, then by success, and then by total threads.
    // If a row is encountered that has an extra column, then skip the
    // defective row. Any other columns from the source jtl aren't considered.

    String header =
        "timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,bytes,allThreads";
    // First, create the correctly ordered file.
    List<String> ordered =
        List.of(
            // #1 because timeStamp is older.
            "1469546803634,496,GET test/thing,200,OK,example 1-1,true,280,2",
            // #2 because timeStamp is more recent.
            "1469546803635,496,GET test/thing,200,OK,example 1-1,true,280,2",
            // #3 because elapsed is greater.
            "1469546803635,497,GET test/thing,200,OK,example 1-1,true,280,2",
            // #4 because label is lexicographically later.
            "1469546803635,497,GET test/thing,200,OK,example 1-1,true,280,2",
            // #5 because threadName is lexicographically later.
            "1469546803635,497,GET test/thing,200,OK,example 1-2,true,280,2",
            // #6 because more bytes were in the response.
            "1469546803635,497,GET test/thing,200,OK,example 1-2,true,281,2",
            // #7 because status code is greater.
            "1469546803635,497,GET test/thing,201,OK,example 1-2,true,281,2",
            // #8 because status message is lexigographically later.
            "1469546803635,497,GET test/thing,201,OKAY,example 1-2,true,281,2",
            // #9 because success false comes after true. (Signed int, true=-1, false = 0;
            "1469546803635,497,GET test/thing,201,OKAY,example 1-2,false,281,2",
            // #10 because total threads is greater.
            "1469546803635,497,GET test/thing,201,OKAY,example 1-2,false,281,3",
            // #10 as well, but is a duplicate.
            "1469546803635,497,GET test/thing,201,OKAY,example 1-2,false,281,3");

    try (TempContent sourceFile = TempContent.withName("source", ".jtl");
        TempContent expectedOrderedFile = TempContent.withName("expected", ".jtl");
        TempContent avroFile = TempContent.withName("converted", ".avro");
        TempContent reconstitutedFile = TempContent.withName("reconstituted", ".jtl")) {
      Files.write(
          expectedOrderedFile.path(), Collections.singleton(header), StandardOpenOption.CREATE);
      Files.write(expectedOrderedFile.path(), ordered, StandardOpenOption.APPEND);

      // Given a file with unordered lines.
      Files.write(sourceFile.path(), List.of(header), StandardOpenOption.CREATE);
      Files.write(sourceFile.path(), ordered.reversed(), StandardOpenOption.APPEND);

      // When converted into avro format using the ordered converter and converted back into a CSV,
      Converter jtl2Avro = new CsvJtlToAvroOrderedConverter();
      jtl2Avro.convert(sourceFile.file(), avroFile.file());

      // Then the data is there and in the correct order.
      Converter avro2Jtl = new AvroToCsvJtlConverter();
      avro2Jtl.convert(avroFile.file(), reconstitutedFile.file());
      assertContentEquals(
          reconstitutedFile.file(),
          expectedOrderedFile.file(),
          "The converter did not order the rows as expected.");
    }
  }

  @Test(expectedExceptions = {AppServerException.class})
  public void testConvertNoHeaderButDefaultColumns() throws IOException {
    // Given a JTL file with no header row but 12 columns,

    // Back in the day, some versions of jmeter did not provide the header row with the CSV, and
    // it only had data for these 12 columns:
    // timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency
    // There's not much use for this now, so lognition will not handle this special case anymore.
    String headerlessContent =
        """
        1766362285195,104,GET /logs/test,200,OK,Thread Group 1-1,text,true,468,10,10,104
        1766362285191,111,PUT /logs/test,200,OK,Thread Group 1-5,text,true,538,10,10,111
        """;
    try (TempContent sourceFile = TempContent.of(headerlessContent);
        TempContent avroFile = TempContent.withName("converted", ".avro");
        TempContent reconstitutedFile = TempContent.withName("reconstituted", ".jtl")) {
      // When converting to avro format and reconstituting back into a CSV,
      Converter jtl2avro = new CsvJtlToAvroOrderedConverter();
      jtl2avro.convert(sourceFile.file(), avroFile.file());

      Converter avro2jtl = new AvroToCsvJtlConverter();
      avro2jtl.convert(avroFile.file(), reconstitutedFile.file());
      // Then the operation should fail because the headers are unknown
    }
  }

  @Test
  public void testConvertRowWithMissingColumnSkipped() throws IOException {
    // If a row is encountered that has a missing column, then skip the
    // defective row.
    File source = createTempFile("sourceMissingColumn", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
      pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
      pw.println("600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599");
    }
    // This jtl is the same as above, but the row with the missing column
    // is removed. The resulting avro files from each should be equal.
    File sourceEffective = createTempFile("sourceEffective", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(sourceEffective.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
      pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
    }

    Converter conv = new CsvJtlToAvroOrderedConverter();
    File actualDest = createTempFile("actual", ".avro");
    conv.convert(source, actualDest);
    File effectiveDest = createTempFile("effective", ".avro");
    conv.convert(source, effectiveDest);

    assertAvroContentEquals(
        actualDest,
        effectiveDest,
        "The converter did not handle a malformed row in the way expected.");
  }

  @Test
  public void testConvertRowWithExtraColumnSkipped() throws IOException {
    // If a row is encountered that has an extra column, then skip the
    // defective row.
    File source = createTempFile("sourceMissingColumn", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
      pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
      pw.println("extra,1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599");
    }
    // This jtl is the same as above, but the row with the extra column
    // is removed. The resulting avro files from each should be equal.
    File sourceEffective = createTempFile("sourceEffective", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(sourceEffective.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
      pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
    }

    Converter conv = new CsvJtlToAvroOrderedConverter();
    File actualDest = createTempFile("actual", ".avro");
    conv.convert(source, actualDest);
    File effectiveDest = createTempFile("effective", ".avro");
    conv.convert(source, effectiveDest);

    assertAvroContentEquals(
        actualDest,
        effectiveDest,
        "The converter did not handle a malformed row in the way expected.");
  }

  @Test(dataProvider = "nonNumericColumnsDp")
  public void testConvertRowNumericColumnsAreNonNumericAreSkipped(String badRow, String whyBad)
      throws IOException {
    // If a row is encountered that has an extra column, then skip the
    // defective row.
    File source = createTempFile("sourceMissingColumn", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
      pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
      pw.println(badRow);
    }
    // This jtl is the same as above, but the row with the extra column
    // is removed. The resulting avro files from each should be equal.
    File sourceEffective = createTempFile("sourceEffective", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(sourceEffective.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
      pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
    }

    Converter conv = new CsvJtlToAvroOrderedConverter();
    File actualDest = createTempFile("actual", ".avro");
    conv.convert(source, actualDest);
    File effectiveDest = createTempFile("effective", ".avro");
    conv.convert(source, effectiveDest);

    assertAvroContentEquals(
        actualDest,
        effectiveDest,
        "The converter did not handle a malformed row, \"" + whyBad + "\" in the way expected.");
  }

  @DataProvider(name = "nonNumericColumnsDp", parallel = true)
  public static Object[][] nonNumericColumnsDp() {
    return new Object[][] {
      new Object[] {
        "words,600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599", "Bad timeStamp"
      },
      new Object[] {
        "9223372036854775808,600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599",
        "timeStamp is longer than a long"
      },
      new Object[] {
        "1469546803889,blah,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599", "Bad elapsed"
      },
      new Object[] {
        "1469546803889,9223372036854775808,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599",
        "elapsed is longer than a long"
      },
      new Object[] {
        "1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,bogus,2,2,599", "bad bytes"
      },
      new Object[] {
        "1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,9223372036854775808,2,2,599",
        "bytes is longer than a long"
      },
      new Object[] {
        "1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,bogus,2,599",
        "bad grpThreads"
      },
      //            new Object[]{"1469546803889,600,GET test/thing,200,OK,example
      // 1-1,text,true,280,9223372036854775808,2,599", "grpThreads longer than a long"},
      //            new Object[]{"1469546803889,600,GET test/thing,200,OK,example
      // 1-1,text,true,280,2,bogus,599", "bad allThreads"},
      new Object[] {
        "1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,2,9223372036854775808,599",
        "allThreads longer than a long"
      },
      new Object[] {
        "1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,bogus", "bad Latency"
      },
      new Object[] {
        "1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,9223372036854775808",
        "Latency longer than a long"
      },
      new Object[] {
        "1469546803889,600,GET test/thing,200,OK,example 1-1,text,yes,280,2,2,599",
        "success is not a boolean"
      },
      new Object[] {
        "1469546803889,600,GET test/thing,200,OK,example 1-1,text,0,280,2,2,599",
        "success is not a boolean"
      },
    };
  }

  @Test(expectedExceptions = {AppServerException.class})
  public void testConvertNoHeaderNotDefaultColumns() throws IOException {
    File source = createTempFile("source", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      // The thread name is removed. This should not be convertible
      // without a header row.
      pw.println("1469546803634,496,GET test/thing,200,OK,text,true,280,2,2,495");
      pw.println("1469546803889,600,GET test/thing,200,OK,text,true,280,2,2,599");
    }

    Converter conv = new CsvJtlToAvroOrderedConverter();
    File actualDest = createTempFile("actual", ".avro");
    conv.convert(source, actualDest);
  }

  @Test(expectedExceptions = {AppServerException.class})
  public void testConvertNoHeaderMoreColumnsThanDefault() throws IOException {
    File source = createTempFile("source", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      // An extra column is added at the end. This should not be
      // convertable without a header row.
      pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495,extra");
      pw.println("1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599,extra");
    }

    Converter conv = new CsvJtlToAvroOrderedConverter();
    File actualDest = createTempFile("actual", ".avro");
    conv.convert(source, actualDest);
  }

  @Test(expectedExceptions = {AppServerException.class})
  public void testConvertRequiredHeaderTimestampMissing() throws IOException {
    File source = createTempFile("source", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
      pw.println("496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
      pw.println("600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599");
    }

    Converter conv = new CsvJtlToAvroOrderedConverter();
    File actualDest = createTempFile("actual", ".avro");
    conv.convert(source, actualDest);
  }

  @Test(expectedExceptions = {AppServerException.class})
  public void testConvertRequiredHeaderElapsedMissing() throws IOException {
    File source = createTempFile("source", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "timeStamp,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
      pw.println("1469546803634,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
      pw.println("1469546803889,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599");
    }

    Converter conv = new CsvJtlToAvroOrderedConverter();
    File actualDest = createTempFile("actual", ".avro");
    conv.convert(source, actualDest);
  }

  @Test(expectedExceptions = {AppServerException.class})
  public void testConvertRequiredHeaderLabelMissing() throws IOException {
    File source = createTempFile("source", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "timeStamp,elapsed,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
      pw.println("1469546803634,496,200,OK,example 1-1,text,true,280,2,2,495");
      pw.println("1469546803889,600,200,OK,example 1-1,text,true,280,2,2,599");
    }

    Converter conv = new CsvJtlToAvroOrderedConverter();
    File actualDest = createTempFile("actual", ".avro");
    conv.convert(source, actualDest);
  }

  @Test(expectedExceptions = {AppServerException.class})
  public void testConvertRequiredHeaderResponseCodeMissing() throws IOException {
    File source = createTempFile("source", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "timeStamp,elapsed,label,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
      pw.println("1469546803634,496,GET test/thing,OK,example 1-1,text,true,280,2,2,495");
      pw.println("1469546803889,600,GET test/thing,OK,example 1-1,text,true,280,2,2,599");
    }

    Converter conv = new CsvJtlToAvroOrderedConverter();
    File actualDest = createTempFile("actual", ".avro");
    conv.convert(source, actualDest);
  }

  @Test(expectedExceptions = {AppServerException.class})
  public void testConvertRequiredHeaderThreadNameMissing() throws IOException {
    File source = createTempFile("source", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "timeStamp,elapsed,label,responseCode,responseMessage,dataType,success,bytes,grpThreads,allThreads,Latency");
      pw.println("1469546803634,496,GET test/thing,200,OK,text,true,280,2,2,495");
      pw.println("1469546803889,600,GET test/thing,200,OK,text,true,280,2,2,599");
    }

    Converter conv = new CsvJtlToAvroOrderedConverter();
    File actualDest = createTempFile("actual", ".avro");
    conv.convert(source, actualDest);
  }

  @Test(expectedExceptions = {AppServerException.class})
  public void testConvertRequiredHeaderSuccessMissing() throws IOException {
    File source = createTempFile("source", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,bytes,grpThreads,allThreads,Latency");
      pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,280,2,2,495");
      pw.println("1469546803889,600,GET test/thing,200,OK,example 1-1,text,280,2,2,599");
    }

    Converter conv = new CsvJtlToAvroOrderedConverter();
    File actualDest = createTempFile("actual", ".avro");
    conv.convert(source, actualDest);
  }

  @Test(expectedExceptions = {AppServerException.class})
  public void testConvertRequiredHeaderBytesMissing() throws IOException {
    File source = createTempFile("source", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,grpThreads,allThreads,Latency");
      pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,2,2,495");
      pw.println("1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,2,2,599");
    }

    Converter conv = new CsvJtlToAvroOrderedConverter();
    File actualDest = createTempFile("actual", ".avro");
    conv.convert(source, actualDest);
  }

  @Test(expectedExceptions = {AppServerException.class})
  public void testConvertRequiredHeaderAllThreadsMissing() throws IOException {
    File source = createTempFile("source", ".jtl");
    try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
        PrintWriter pw = new PrintWriter(bw)) {
      pw.println(
          "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,Latency");
      pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,495");
      pw.println("1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,2,599");
    }

    Converter conv = new CsvJtlToAvroOrderedConverter();
    File actualDest = createTempFile("actual", ".avro");
    conv.convert(source, actualDest);
  }
}
