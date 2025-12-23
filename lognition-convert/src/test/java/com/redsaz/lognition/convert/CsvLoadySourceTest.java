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

public class CsvLoadySourceTest {

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
      Samples samples = CsvLoadySource.readLoadyFile(tc.file());
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
  public void testLoadyFile() {
    String content =
        """
        completed_at_ms,duration_ms,fail,status,bytes_up,bytes_down,call,label,thread
        1244,10,0,200,0,400,GET example/abcd1234,GET example/{itemId},1
        1355,12,1,error: connection refused,0,123,GET fail/323,GET fail/{id},1
        1587,20,0,200,0,667,GET example/56789abc,GET example/{itemId},2
        """;

    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvLoadySource.readLoadyFile(tc.file());

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

      assertEquals(
          samples.getEarliestMillis(),
          1234L,
          "Earliest should be timestamp from epoch when the earliest start happened (completed_at minus duration).");
      assertEquals(
          samples.getLatestMillis(),
          1587L,
          "Latest should be timestamp from epoch when the latest finish happened.");
      assertSame(samples.getEarliestSample(), samples.getSamples().getFirst());
      assertSame(samples.getLatestSample(), samples.getSamples().getLast());
      assertEquals(samples.getLabels(), List.of("GET example/{itemId}", "GET fail/{id}"));
      assertEquals(samples.getThreadNames(), List.of("1", "2"));
    }
  }

  @Test
  public void testLoadyFileNoLabelShouldSucceedWithEmptyLabel() {
    String content =
        """
        completed_at_ms,duration_ms,fail,status,bytes_up,bytes_down,call,label,thread
        1244,10,0,200,0,400,GET http://127.0.0.1:8080/example/abcd1234,,1
        1355,12,1,error: connection refused,0,123,GET http://127.0.0.1:8080/fail/323,,1
        1587,20,0,200,0,667,GET http://127.0.0.1:8080/example/56789abc,,2
        """;

    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvLoadySource.readLoadyFile(tc.file());

      assertEquals(samples.getSamples().size(), 3);
      Sample first = samples.getSamples().getFirst();
      // A Sample offset has the earliest call start at 0, and is when the call started, not when it
      // ended like in Loady.
      assertEquals(first.getOffset(), 0);
      assertEquals(first.getDuration(), 10);
      assertEquals(first.getLabel(), "");
      assertEquals(first.getStatusCode(), "200");
      assertEquals(first.getThreadName(), "1");
      assertTrue(first.isSuccess());
      assertEquals(first.getResponseBytes(), 400);
      assertEquals(first.getTotalThreads(), 2);

      Sample next = samples.getSamples().get(1);
      assertEquals(next.getOffset(), 109);
      assertEquals(next.getDuration(), 12);
      assertEquals(next.getLabel(), "");
      assertEquals(next.getStatusCode(), "error: connection refused");
      assertEquals(next.getThreadName(), "1");
      assertFalse(next.isSuccess());
      assertEquals(next.getResponseBytes(), 123);
      assertEquals(next.getTotalThreads(), 2);

      Sample last = samples.getSamples().getLast();
      assertEquals(last.getOffset(), 333);
      assertEquals(last.getDuration(), 20);
      assertEquals(last.getLabel(), "");
      assertEquals(last.getStatusCode(), "200");
      assertEquals(last.getThreadName(), "2");
      assertTrue(last.isSuccess());
      assertEquals(last.getResponseBytes(), 667);
      assertEquals(last.getTotalThreads(), 2);

      assertEquals(
          samples.getEarliestMillis(),
          1234L,
          "Earliest should be timestamp from epoch when the earliest start happened (completed_at minus duration).");
      assertEquals(
          samples.getLatestMillis(),
          1587L,
          "Latest should be timestamp from epoch when the latest finish happened.");
      assertSame(samples.getEarliestSample(), samples.getSamples().getFirst());
      assertSame(samples.getLatestSample(), samples.getSamples().getLast());
      assertEquals(samples.getLabels(), List.of(""));
      assertEquals(samples.getThreadNames(), List.of("1", "2"));
    }
  }

  @Test
  public void testLoadyFileNoLabelShouldSucceedWithBlankLabel() {
    String content =
        """
        completed_at_ms,duration_ms,fail,status,bytes_up,bytes_down,call,thread
        1244,10,0,200,0,400,GET example/abcd1234,1
        1355,12,1,error: connection refused,0,123,GET fail/323,1
        1587,20,0,200,0,667,GET example/56789abc,2
        """;

    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvLoadySource.readLoadyFile(tc.file());

      assertEquals(samples.getSamples().size(), 3);
      Sample first = samples.getSamples().getFirst();
      // A Sample offset has the earliest call start at 0, and is when the call started, not when it
      // ended like in Loady.
      assertEquals(first.getOffset(), 0);
      assertEquals(first.getDuration(), 10);
      assertEquals(first.getLabel(), "");
      assertEquals(first.getStatusCode(), "200");
      assertEquals(first.getThreadName(), "1");
      assertTrue(first.isSuccess());
      assertEquals(first.getResponseBytes(), 400);
      assertEquals(first.getTotalThreads(), 2);

      Sample next = samples.getSamples().get(1);
      assertEquals(next.getOffset(), 109);
      assertEquals(next.getDuration(), 12);
      assertEquals(next.getLabel(), "");
      assertEquals(next.getStatusCode(), "error: connection refused");
      assertEquals(next.getThreadName(), "1");
      assertFalse(next.isSuccess());
      assertEquals(next.getResponseBytes(), 123);
      assertEquals(next.getTotalThreads(), 2);

      Sample last = samples.getSamples().getLast();
      assertEquals(last.getOffset(), 333);
      assertEquals(last.getDuration(), 20);
      assertEquals(last.getLabel(), "");
      assertEquals(last.getStatusCode(), "200");
      assertEquals(last.getThreadName(), "2");
      assertTrue(last.isSuccess());
      assertEquals(last.getResponseBytes(), 667);
      assertEquals(last.getTotalThreads(), 2);

      assertEquals(
          samples.getEarliestMillis(),
          1234L,
          "Earliest should be timestamp from epoch when the earliest start happened (completed_at minus duration).");
      assertEquals(
          samples.getLatestMillis(),
          1587L,
          "Latest should be timestamp from epoch when the latest finish happened.");
      assertSame(samples.getEarliestSample(), samples.getSamples().getFirst());
      assertSame(samples.getLatestSample(), samples.getSamples().getLast());
      assertEquals(samples.getLabels(), List.of(""));
      assertEquals(samples.getThreadNames(), List.of("1", "2"));
    }
  }

  @Test
  public void testLoadyFileNoEntriesShouldSucceedWithNoSamples() {
    String content =
        """
        completed_at_ms,duration_ms,fail,status,bytes_up,bytes_down,call,label,thread
        """;

    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvLoadySource.readLoadyFile(tc.file());
      assertEquals(samples.getSamples().size(), 0);
    }
  }

  @Test
  public void testEmptyFileShouldSucceedWithNoSamples() {
    String content = "";

    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvLoadySource.readLoadyFile(tc.file());
      assertEquals(samples.getSamples().size(), 0);
    }
  }

  @Test
  public void testNonLoadyDoesNotConvert() {
    String content =
        """
        ended_at_ms,amount_ms,passed,status_code,download_bytes,name,user_id
        1244,10,1,200,400,GET example/{itemId},1
        1587,20,1,200,667,GET example/{itemId},2
        """;

    try (TempContent tc = TempContent.of(content)) {
      expectThrows(AppServerException.class, () -> CsvLoadySource.readLoadyFile(tc.file()));
    }
  }

  @Test
  public void testLoadyNoHeadersDoesNotConvert() {
    String content =
        """
        1244,10,0,200,0,400,GET example/abcd1234,GET example/{itemId},1
        1587,20,0,200,0,667,GET example/56789abc,GET example/{itemId},2
        """;

    try (TempContent tc = TempContent.of(content)) {
      expectThrows(AppServerException.class, () -> CsvLoadySource.readLoadyFile(tc.file()));
    }
  }
}
