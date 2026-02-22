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

import static com.redsaz.lognition.convert.ConverterBaseTest.assertContentEquals;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import com.redsaz.lognition.api.exceptions.AppServerException;
import com.redsaz.lognition.api.model.Sample;
import java.io.IOException;
import java.util.List;
import org.testng.annotations.Test;

public class CsvSamplesReaderTest {

  @Test
  public void testRealisticLoadyFile() throws IOException {
    // Given a Loady test results log file,
    String content =
        """
        completed_at_ms,duration_ms,fail,status,bytes_up,bytes_down,call,label,thread
        1766362327212,101,0,200,0,362,GET http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key7=val7,GET logs/test,8
        1766362327212,102,0,200,55,464,POST http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key9=val9,POST logs/test,9
        1766362327213,103,0,200,0,362,GET http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key5=val5,GET logs/test,5
        1766362327214,0,1,error: connection refused,0,0,GET http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key12=val12,GET logs/test,5
        1766362327218,107,0,200,0,362,GET http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key3=val3,GET logs/test,3
        1766362327225,115,0,200,0,362,GET http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key6=val6,GET logs/test,6
        1766362327227,116,0,200,0,362,GET http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key2=val2,GET logs/test,2
        1766362327228,117,0,200,0,362,GET http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key4=val4,GET logs/test,4
        1766362327228,118,1,400,0,387,GET http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key8=val8&status=400,GET logs/test,7
        1766362327233,123,0,200,55,463,PUT http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key0=val0,PUT logs/test,1
        1766362327247,137,0,200,0,362,GET http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key1=val1,GET logs/test,0
        1766362327309,97,0,200,62,472,PUT http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key11=val11,PUT logs/test,9
        1766362327318,105,1,404,0,389,GET http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key10=val10&status=404,GET logs/test,8
        1766362327324,95,0,200,0,362,GET http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key3=val3,GET logs/test,4
        """;

    try (TempContent tc = TempContent.of(content);
        TempContent reconstitutedFile = TempContent.withName("reconstituted", ".log")) {
      // When the data is read into a Samples structure and then output back as a Loady log file,
      Samples samples = CsvSamplesReader.readSamples(tc.path());
      SamplesWriter writer = new CsvLoadySamplesWriter();
      writer.write(samples, reconstitutedFile.file());

      // Then the reconstituted log data should match the original data, but without bytes_up or
      // call columns (though maybe some day that'd be nice) and is ordered by when the calls
      // started (though maybe some day it'd be good to be ordered by completion time):
      // completed_at_ms,duration_ms,fail,status,bytes_down,label,thread
      String expectedStr =
          """
          completed_at_ms,duration_ms,fail,status,bytes_down,label,thread
          1766362327212,102,0,200,464,POST logs/test,9
          1766362327213,103,0,200,362,GET logs/test,5
          1766362327225,115,0,200,362,GET logs/test,6
          1766362327228,118,1,400,387,GET logs/test,7
          1766362327233,123,0,200,463,PUT logs/test,1
          1766362327247,137,0,200,362,GET logs/test,0
          1766362327212,101,0,200,362,GET logs/test,8
          1766362327218,107,0,200,362,GET logs/test,3
          1766362327227,116,0,200,362,GET logs/test,2
          1766362327228,117,0,200,362,GET logs/test,4
          1766362327309,97,0,200,472,PUT logs/test,9
          1766362327318,105,1,404,389,GET logs/test,8
          1766362327214,0,1,error: connection refused,0,GET logs/test,5
          1766362327324,95,0,200,362,GET logs/test,4
          """;
      assertContentEquals(reconstitutedFile.content(), expectedStr, "Reconstituted Loady data");
    }
  }

  @Test
  public void testLoadyFile() throws IOException {
    String content =
        """
        completed_at_ms,duration_ms,fail,status,bytes_up,bytes_down,call,label,thread
        1244,10,0,200,0,400,GET example/abcd1234,GET example/{itemId},1
        1355,12,1,error: connection refused,0,123,GET fail/323,GET fail/{id},1
        1587,20,0,200,0,667,GET example/56789abc,GET example/{itemId},2
        """;

    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvSamplesReader.readSamples(tc.path());

      assertEquals(samples.getSamples().size(), 3);
      Sample first = samples.getSamples().getFirst();
      // A Sample offset has the earliest call start at 0, and is when the call started, not when it
      // ended like in Loady.
      assertEquals(first.getOffset(), 0);
      assertEquals(first.getDuration(), 10);
      assertEquals(first.getLabel(), "GET example/{itemId}");
      assertEquals(first.getStatusCode(), "200");
      assertEquals(first.getThreadName(), "1");
      assertTrue(first.isSuccess());
      assertEquals(first.getResponseBytes(), 400);
      assertEquals(first.getTotalThreads(), 2);

      Sample next = samples.getSamples().get(1);
      assertEquals(next.getOffset(), 109);
      assertEquals(next.getDuration(), 12);
      assertEquals(next.getLabel(), "GET fail/{id}");
      assertEquals(next.getStatusCode(), "error: connection refused");
      assertEquals(next.getThreadName(), "1");
      assertFalse(next.isSuccess());
      assertEquals(next.getResponseBytes(), 123);
      assertEquals(next.getTotalThreads(), 2);

      Sample last = samples.getSamples().getLast();
      assertEquals(last.getOffset(), 333);
      assertEquals(last.getDuration(), 20);
      assertEquals(last.getLabel(), "GET example/{itemId}");
      assertEquals(last.getStatusCode(), "200");
      assertEquals(last.getThreadName(), "2");
      assertTrue(last.isSuccess());
      assertEquals(last.getResponseBytes(), 667);
      assertEquals(last.getTotalThreads(), 2);

      assertSame(samples.getEarliestSample(), samples.getSamples().getFirst());
      assertSame(samples.getLatestSample(), samples.getSamples().getLast());
      assertEquals(samples.getLabels(), List.of("GET example/{itemId}", "GET fail/{id}"));
      assertEquals(samples.getThreadNames(), List.of("1", "2"));
    }
  }

  @Test
  public void testLoadyHeadersResultInLoadySource() throws IOException {
    /// When, in millis since the Unix epoch UTC, the call completed.
    String content =
        """
        completed_at_ms,duration_ms,fail,status,bytes_up,bytes_down,call,label,thread
        1244,10,0,200,0,400,GET example/abcd1234,GET example/{itemId},1
        1587,20,0,200,0,667,GET example/56789abc,GET example/{itemId},2
        """;

    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvSamplesReader.readSamples(tc.path());

      assertEquals(samples.getSamples().size(), 2);
      Sample first = samples.getSamples().getFirst();
      assertEquals(first.getOffset(), 0);
      assertEquals(first.getDuration(), 10);
      assertEquals(first.getLabel(), "GET example/{itemId}");
      assertEquals(first.getStatusCode(), "200");
      assertEquals(first.getThreadName(), "1");
      assertTrue(first.isSuccess());
      assertEquals(first.getResponseBytes(), 400);
      assertEquals(first.getTotalThreads(), 2);

      Sample last = samples.getSamples().getLast();
      assertEquals(last.getOffset(), 333);
      assertEquals(last.getDuration(), 20);
      assertEquals(last.getLabel(), "GET example/{itemId}");
      assertEquals(last.getStatusCode(), "200");
      assertEquals(last.getThreadName(), "2");
      assertTrue(last.isSuccess());
      assertEquals(last.getResponseBytes(), 667);
      assertEquals(last.getTotalThreads(), 2);
    }
  }

  @Test
  public void testLoadyConvertRowWithMissingColumnSkipped() throws IOException {
    String content =
        """
        completed_at_ms,duration_ms,fail,status,bytes_up,bytes_down,call,label,thread
        1244,10,0,200,0,400,GET example/abcd1234,GET example/{itemId},1
        1355,12,1,error: connection refused,0,123,GET fail/323,GET fail/{id}
        1587,20,0,200,0,667,GET example/56789abc,GET example/{itemId},2
        """;

    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvSamplesReader.readSamples(tc.path());

      assertEquals(samples.getSamples().size(), 2);
      Sample first = samples.getSamples().getFirst();
      // A Sample offset has the earliest call start at 0, and is when the call started, not when it
      // ended like in Loady.
      assertEquals(first.getOffset(), 0);
      assertEquals(first.getDuration(), 10);
      assertEquals(first.getLabel(), "GET example/{itemId}");
      assertEquals(first.getStatusCode(), "200");
      assertEquals(first.getThreadName(), "1");
      assertTrue(first.isSuccess());
      assertEquals(first.getResponseBytes(), 400);
      assertEquals(first.getTotalThreads(), 2);

      // The middle sample is skipped because it had one less column than normal.

      Sample last = samples.getSamples().getLast();
      assertEquals(last.getOffset(), 333);
      assertEquals(last.getDuration(), 20);
      assertEquals(last.getLabel(), "GET example/{itemId}");
      assertEquals(last.getStatusCode(), "200");
      assertEquals(last.getThreadName(), "2");
      assertTrue(last.isSuccess());
      assertEquals(last.getResponseBytes(), 667);
      assertEquals(last.getTotalThreads(), 2);

      assertSame(samples.getEarliestSample(), samples.getSamples().getFirst());
      assertSame(samples.getLatestSample(), samples.getSamples().getLast());
      assertEquals(samples.getLabels(), List.of("GET example/{itemId}"));
      assertEquals(samples.getThreadNames(), List.of("1", "2"));
    }
  }


  @Test
  public void testLoadyConvertRowWithExtraColumnSkipped() throws IOException {
    String content =
        """
        completed_at_ms,duration_ms,fail,status,bytes_up,bytes_down,call,label,thread
        1244,10,0,200,0,400,GET example/abcd1234,GET example/{itemId},1
        1355,12,1,error: connection refused,0,123,GET fail/323,GET fail/{id},1,extra
        1587,20,0,200,0,667,GET example/56789abc,GET example/{itemId},2
        """;

    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvSamplesReader.readSamples(tc.path());

      assertEquals(samples.getSamples().size(), 2);
      Sample first = samples.getSamples().getFirst();
      // A Sample offset has the earliest call start at 0, and is when the call started, not when it
      // ended like in Loady.
      assertEquals(first.getOffset(), 0);
      assertEquals(first.getDuration(), 10);
      assertEquals(first.getLabel(), "GET example/{itemId}");
      assertEquals(first.getStatusCode(), "200");
      assertEquals(first.getThreadName(), "1");
      assertTrue(first.isSuccess());
      assertEquals(first.getResponseBytes(), 400);
      assertEquals(first.getTotalThreads(), 2);

      // The middle sample is skipped because it had one more column than normal.

      Sample last = samples.getSamples().getLast();
      assertEquals(last.getOffset(), 333);
      assertEquals(last.getDuration(), 20);
      assertEquals(last.getLabel(), "GET example/{itemId}");
      assertEquals(last.getStatusCode(), "200");
      assertEquals(last.getThreadName(), "2");
      assertTrue(last.isSuccess());
      assertEquals(last.getResponseBytes(), 667);
      assertEquals(last.getTotalThreads(), 2);

      assertSame(samples.getEarliestSample(), samples.getSamples().getFirst());
      assertSame(samples.getLatestSample(), samples.getSamples().getLast());
      assertEquals(samples.getLabels(), List.of("GET example/{itemId}"));
      assertEquals(samples.getThreadNames(), List.of("1", "2"));
    }
  }

  @Test
  public void testRealisticJtlFile() throws IOException {
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

    // Given a Jmeter test results log (JTL) file,
    try (TempContent tc = TempContent.of(content);
        TempContent reconstitutedFile = TempContent.withName("reconstituted", ".jtl")) {
      // When the data is read into a Samples structure and then output back as a JTL file,
      Samples samples = CsvSamplesReader.readSamples(tc.path());
      CsvJtlSamplesWriter writer = new CsvJtlSamplesWriter();
      writer.write(samples, reconstitutedFile.file());

      // Then the reconstituted log data should match the original data, but ordered by timestamp
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
    }
  }

  @Test
  public void testNonJtlNonLoadySourceResultsInNoSource() {
    String content =
        """
        ended_at_ms,amount_ms,passed,status_code,download_bytes,name,user_id
        1244,10,1,200,400,GET example/{itemId},1
        1587,20,1,200,667,GET example/{itemId},2
        """;

    try (TempContent tc = TempContent.of(content)) {
      expectThrows(AppServerException.class, () -> CsvSamplesReader.readSamples(tc.path()));
    }
  }

  @Test
  public void testJtlNoHeadersResultInNoSource() throws IOException {
    // Given a jtl file (or any file really) with no header,
    String content =
        """
        1234,10,GET example/{itemId},200,1,true,400,2
        1567,20,GET example/{itemId},200,2,true,667,2
        """;
    try (TempContent tc = TempContent.of(content)) {
      // When samples are attempted to be loaded from it,
      Samples samples = CsvSamplesReader.readSamples(tc.path());

      // Then an exception is thrown explaining it could not be converted
    } catch (AppServerException ex) {
      assertTrue(ex.getMessage().startsWith("Cannot find Sample deserializer for "));
    }
  }

  @Test
  public void testJtlConvertRowWithMissingColumnSkipped() throws IOException {
    // If a row is encountered that has a missing column, then skip the defective row.
    String content =
        """
          timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,bytes,allThreads
          1469546803634,496,GET test/thing,200,OK,example 1-1,true,280,2
          1469546803635,496,GET test/thing,200,OK,example 1-1,true,280
          1469546803636,497,GET test/thing,200,OK,example 1-1,true,280,2
          """;
    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvSamplesReader.readSamples(tc.path());

      assertEquals(samples.getSamples().size(), 2);
      Sample first = samples.getSamples().getFirst();
      // A Sample offset has the earliest call start at 0, when the call started (not when it ends)
      assertEquals(first.getOffset(), 0L);
      assertEquals(first.getDuration(), 496L);
      assertEquals(first.getLabel(), "GET test/thing");
      assertEquals(first.getStatusCode(), "200");
      assertEquals(first.getThreadName(), "example 1-1");
      assertTrue(first.isSuccess());
      assertEquals(first.getResponseBytes(), 280L);
      assertEquals(first.getTotalThreads(), 2L);

      Sample last = samples.getSamples().getLast();
      assertEquals(last.getOffset(), 2L);
      assertEquals(last.getDuration(), 497L);
      assertEquals(last.getLabel(), "GET test/thing");
      assertEquals(last.getStatusCode(), "200");
      assertEquals(last.getThreadName(), "example 1-1");
      assertTrue(last.isSuccess());
      assertEquals(last.getResponseBytes(), 280L);
      assertEquals(last.getTotalThreads(), 2);
    }
  }

  @Test
  public void testConvertRowWithExtraColumnSkipped() throws IOException {
    // If a row is encountered that has an extra column, then skip the defective row.
    String content =
        """
          timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,bytes,allThreads
          1469546803634,496,GET test/thing,200,OK,example 1-1,true,280,2
          1469546803635,496,GET test/thing,200,OK,example 1-1,true,280,2,extra
          1469546803636,497,GET test/thing,200,OK,example 1-1,true,280,2
          """;
    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvSamplesReader.readSamples(tc.path());

      assertEquals(samples.getSamples().size(), 2);
      Sample first = samples.getSamples().getFirst();
      // A Sample offset has the earliest call start at 0, when the call started (not when it ends)
      assertEquals(first.getOffset(), 0L);
      assertEquals(first.getDuration(), 496L);
      assertEquals(first.getLabel(), "GET test/thing");
      assertEquals(first.getStatusCode(), "200");
      assertEquals(first.getThreadName(), "example 1-1");
      assertTrue(first.isSuccess());
      assertEquals(first.getResponseBytes(), 280L);
      assertEquals(first.getTotalThreads(), 2L);

      Sample last = samples.getSamples().getLast();
      assertEquals(last.getOffset(), 2L);
      assertEquals(last.getDuration(), 497L);
      assertEquals(last.getLabel(), "GET test/thing");
      assertEquals(last.getStatusCode(), "200");
      assertEquals(last.getThreadName(), "example 1-1");
      assertTrue(last.isSuccess());
      assertEquals(last.getResponseBytes(), 280L);
      assertEquals(last.getTotalThreads(), 2);
    }
  }
}
