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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import com.redsaz.lognition.api.exceptions.AppServerException;
import com.redsaz.lognition.api.model.Sample;
import java.util.List;
import org.testng.annotations.Test;

public class CsvAutoSourceTest {

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
      Samples samples = CsvAutoSource.loadSamples(tc.file());

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
  public void testLoadyHeadersResultInLoadySource() {
    /// When, in millis since the Unix epoch UTC, the call completed.
    String content =
        """
        completed_at_ms,duration_ms,fail,status,bytes_up,bytes_down,call,label,thread
        1244,10,0,200,0,400,GET example/abcd1234,GET example/{itemId},1
        1587,20,0,200,0,667,GET example/56789abc,GET example/{itemId},2
        """;

    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvAutoSource.loadSamples(tc.file());

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
  public void testNonJtlNonLoadySourceResultsInNoSource() {
    String content =
        """
        ended_at_ms,amount_ms,passed,status_code,download_bytes,name,user_id
        1244,10,1,200,400,GET example/{itemId},1
        1587,20,1,200,667,GET example/{itemId},2
        """;

    try (TempContent tc = TempContent.of(content)) {
      expectThrows(AppServerException.class, () -> CsvAutoSource.loadSamples(tc.file()));
    }
  }

  @Test
  public void testJtlNoHeadersResultInNoSource() {
    String content =
        """
        timeStamp,elapsed,label,responseCode,threadName,success,bytes,allThreads
        1234,10,GET example/{itemId},200,1,true,400,2
        1567,20,GET example/{itemId},200,2,true,667,2
        """;
    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvAutoSource.loadSamples(tc.file());

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
}
