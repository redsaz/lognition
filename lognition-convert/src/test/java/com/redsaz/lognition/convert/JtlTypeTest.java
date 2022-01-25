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
import static org.testng.Assert.assertSame;

import com.redsaz.lognition.convert.model.jmeter.CsvJtlRow;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the JtlType class.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class JtlTypeTest {

  @Test(dataProvider = "convertDp")
  public void testConvert(JtlType jtlType, String input, Object expected) {
    Object actual = jtlType.convert(input);
    assertEquals(
        actual.getClass(), expected.getClass(), "Wrong conversion class for " + jtlType.name());
    assertEquals(actual, expected, "Wrong conversion value for " + jtlType.name());
  }

  @Test(dataProvider = "csvNameDp")
  public void testCsvName(JtlType jtlType, String expected) {
    String actual = jtlType.csvName();
    assertEquals(actual, expected, "Wrong csvName value for " + jtlType.name());
  }

  @Test
  public void testPutIn() {
    CsvJtlRow expected =
        new CsvJtlRow(
            1234L,
            10L,
            "Label",
            "200",
            "OK",
            "Thread-1",
            "text",
            Boolean.TRUE,
            "This isn't normally here on success.",
            100L,
            10L,
            10,
            11,
            "http://www.redsaz.com",
            "file.csv",
            11,
            9,
            "UTF-8",
            1,
            0,
            "redsaz",
            1,
            "bob=1");
    CsvJtlRow actual = new CsvJtlRow();
    JtlType.TIMESTAMP.putIn(actual, "1234");
    JtlType.ELAPSED.putIn(actual, "10");
    JtlType.LABEL.putIn(actual, "Label");
    JtlType.RESPONSE_CODE.putIn(actual, "200");
    JtlType.RESPONSE_MESSAGE.putIn(actual, "OK");
    JtlType.THREAD_NAME.putIn(actual, "Thread-1");
    JtlType.DATA_TYPE.putIn(actual, "text");
    JtlType.SUCCESS.putIn(actual, "true");
    JtlType.FAILURE_MESSAGE.putIn(actual, "This isn't normally here on success.");
    JtlType.BYTES.putIn(actual, "100");
    JtlType.SENT_BYTES.putIn(actual, "10");
    JtlType.GRP_THREADS.putIn(actual, "10");
    JtlType.ALL_THREADS.putIn(actual, "11");
    JtlType.URL.putIn(actual, "http://www.redsaz.com");
    JtlType.FILENAME.putIn(actual, "file.csv");
    JtlType.LATENCY.putIn(actual, "11");
    JtlType.CONNECT.putIn(actual, "9");
    JtlType.ENCODING.putIn(actual, "UTF-8");
    JtlType.SAMPLE_COUNT.putIn(actual, "1");
    JtlType.ERROR_COUNT.putIn(actual, "0");
    JtlType.HOSTNAME.putIn(actual, "redsaz");
    JtlType.IDLE_TIME.putIn(actual, "1");
    JtlType.VARIABLES.putIn(actual, "bob=1");
    assertEquals(actual.getAllThreads(), expected.getAllThreads(), "AllThreads");
    assertEquals(actual.getBytes(), expected.getBytes(), "Bytes");
    assertEquals(actual.getConnect(), expected.getConnect(), "Connect");
    assertEquals(actual.getDataType(), expected.getDataType(), "DataType");
    assertEquals(actual.getElapsed(), expected.getElapsed(), "Elapsed");
    assertEquals(actual.getEncoding(), expected.getEncoding(), "Encoding");
    assertEquals(actual.getErrorCount(), expected.getErrorCount(), "ErrorCount");
    assertEquals(actual.getFailureMessage(), expected.getFailureMessage(), "FailureMessage");
    assertEquals(actual.getFilename(), expected.getFilename(), "Filename");
    assertEquals(actual.getGrpThreads(), expected.getGrpThreads(), "GrpThreads");
    assertEquals(actual.getHostname(), expected.getHostname(), "Hostname");
    assertEquals(actual.getIdleTime(), expected.getIdleTime(), "IdleTime");
    assertEquals(actual.getLabel(), expected.getLabel(), "Label");
    assertEquals(actual.getLatency(), expected.getLatency(), "Latency");
    assertEquals(actual.getResponseCode(), expected.getResponseCode(), "ResponseCode");
    assertEquals(actual.getResponseMessage(), expected.getResponseMessage(), "ResponseMessage");
    assertEquals(actual.getSampleCount(), expected.getSampleCount(), "SampleCount");
    assertEquals(actual.getSchema(), expected.getSchema(), "Schema");
    assertEquals(actual.getSentBytes(), expected.getSentBytes(), "SentBytes");
    assertEquals(actual.getSuccess(), expected.getSuccess(), "Success");
    assertEquals(actual.getThreadName(), expected.getThreadName(), "ThreadName");
    assertEquals(actual.getTimeStamp(), expected.getTimeStamp(), "TimeStamp");
    assertEquals(actual.getURL(), expected.getURL(), "URL");
    assertEquals(actual.getVariables(), expected.getVariables(), "Variables");
  }

  @Test(dataProvider = "fromHeaderDp")
  public void testFromHeader(JtlType expected, String input) {
    JtlType actual = JtlType.fromHeader(input);
    assertSame(actual, expected, "Wrong JtlType for input: " + input);
  }

  @DataProvider(name = "convertDp", parallel = true)
  public static Object[][] convertDp() {
    return new Object[][] {
      {JtlType.TIMESTAMP, "999999999", 999999999L},
      {JtlType.ELAPSED, "999999999", 999999999L},
      {JtlType.LABEL, "LabelTest", "LabelTest"},
      {JtlType.RESPONSE_CODE, "200", "200"},
      {JtlType.RESPONSE_MESSAGE, "Success", "Success"},
      {JtlType.THREAD_NAME, "Name of Thread", "Name of Thread"},
      {JtlType.DATA_TYPE, "text", "text"},
      {JtlType.SUCCESS, "true", true},
      {JtlType.FAILURE_MESSAGE, "You Lost!", "You Lost!"},
      {JtlType.BYTES, "999999999", 999999999L},
      {JtlType.SENT_BYTES, "999999999", 999999999L},
      {JtlType.GRP_THREADS, "88888888", 88888888},
      {JtlType.ALL_THREADS, "88888888", 88888888},
      {JtlType.URL, "http://www.redsaz.com", "http://www.redsaz.com"},
      {JtlType.FILENAME, "Filename time", "Filename time"},
      {JtlType.LATENCY, "88888888", 88888888},
      {JtlType.CONNECT, "88888888", 88888888},
      {JtlType.ENCODING, "UTF8", "UTF8"},
      {JtlType.SAMPLE_COUNT, "88888888", 88888888},
      {JtlType.ERROR_COUNT, "88888888", 88888888},
      {JtlType.HOSTNAME, "redsaz", "redsaz"},
      {JtlType.IDLE_TIME, "88888888", 88888888},
      {JtlType.VARIABLES, "bob=1", "bob=1"}
    };
  }

  @DataProvider(name = "csvNameDp", parallel = true)
  public static Object[][] csvNameDp() {
    return new Object[][] {
      {JtlType.TIMESTAMP, "timeStamp"},
      {JtlType.ELAPSED, "elapsed"},
      {JtlType.LABEL, "label"},
      {JtlType.RESPONSE_CODE, "responseCode"},
      {JtlType.RESPONSE_MESSAGE, "responseMessage"},
      {JtlType.THREAD_NAME, "threadName"},
      {JtlType.DATA_TYPE, "dataType"},
      {JtlType.SUCCESS, "success"},
      {JtlType.FAILURE_MESSAGE, "failureMessage"},
      {JtlType.BYTES, "bytes"},
      {JtlType.SENT_BYTES, "sentBytes"},
      {JtlType.GRP_THREADS, "grpThreads"},
      {JtlType.ALL_THREADS, "allThreads"},
      {JtlType.URL, "URL"},
      {JtlType.FILENAME, "Filename"},
      {JtlType.LATENCY, "Latency"},
      {JtlType.CONNECT, "connect"},
      {JtlType.ENCODING, "encoding"},
      {JtlType.SAMPLE_COUNT, "SampleCount"},
      {JtlType.ERROR_COUNT, "ErrorCount"},
      {JtlType.HOSTNAME, "Hostname"},
      {JtlType.IDLE_TIME, "IdleTime"},
      {JtlType.VARIABLES, "Variables"}
    };
  }

  @DataProvider(name = "fromHeaderDp", parallel = true)
  public static Object[][] fromHeaderDp() {
    return new Object[][] {
      {JtlType.TIMESTAMP, "timeStamp"},
      {JtlType.ELAPSED, "elapsed"},
      {JtlType.LABEL, "label"},
      {JtlType.RESPONSE_CODE, "responseCode"},
      {JtlType.RESPONSE_MESSAGE, "responseMessage"},
      {JtlType.THREAD_NAME, "threadName"},
      {JtlType.DATA_TYPE, "dataType"},
      {JtlType.SUCCESS, "success"},
      {JtlType.FAILURE_MESSAGE, "failureMessage"},
      {JtlType.BYTES, "bytes"},
      {JtlType.SENT_BYTES, "sentBytes"},
      {JtlType.GRP_THREADS, "grpThreads"},
      {JtlType.ALL_THREADS, "allThreads"},
      {JtlType.URL, "URL"},
      {JtlType.FILENAME, "Filename"},
      {JtlType.LATENCY, "Latency"},
      {JtlType.CONNECT, "connect"},
      {JtlType.ENCODING, "encoding"},
      {JtlType.SAMPLE_COUNT, "SampleCount"},
      {JtlType.ERROR_COUNT, "ErrorCount"},
      {JtlType.HOSTNAME, "Hostname"},
      {JtlType.IDLE_TIME, "IdleTime"},
      {JtlType.VARIABLES, "Variables"}
    };
  }
}
