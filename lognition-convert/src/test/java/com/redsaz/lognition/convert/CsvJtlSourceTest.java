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

import com.redsaz.lognition.api.model.Sample;
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
public class CsvJtlSourceTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvJtlSourceTest.class);

  @Test
  public void testConvert() {
    String content =
        """
        timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,bytes,allThreads
        1469546803634,496,GET test/thing,200,OK,example 1-1,true,280,2
        1469546803635,496,GET test/thing,200,OK,example 1-1,true,280,2
        1469546803635,497,GET test/thing,200,OK,example 1-1,true,280,2
        1469546803635,497,GET test/thing,200,OK,example 1-1,true,280,2
        1469546803635,497,GET test/thing,200,OK,example 1-2,true,280,2
        1469546803635,497,GET test/thing,200,OK,example 1-2,true,281,2
        1469546803635,497,GET test/thing,201,OK,example 1-2,true,281,2
        1469546803635,497,GET test/thing,201,OKAY,example 1-2,true,281,2
        1469546803635,497,GET test/thing,201,OKAY,example 1-2,false,281,2
        1469546803635,497,GET test/thing,201,OKAY,example 1-2,false,281,3
        1469546803635,497,GET test/thing,201,OKAY,example 1-2,false,281,3
        """;
    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvJtlSource.readJtlFile(tc.file());

      assertEquals(samples.getSamples().size(), 11);
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

      Sample next = samples.getSamples().get(1);
      assertEquals(next.getOffset(), 1L);
      assertEquals(next.getDuration(), 496L);
      assertEquals(next.getLabel(), "GET test/thing");
      assertEquals(next.getStatusCode(), "200");
      assertEquals(next.getThreadName(), "example 1-1");
      assertTrue(next.isSuccess());
      assertEquals(next.getResponseBytes(), 280L);
      assertEquals(next.getTotalThreads(), 2L);

      Sample last = samples.getSamples().getLast();
      assertEquals(last.getOffset(), 1L);
      assertEquals(last.getDuration(), 497L);
      assertEquals(last.getLabel(), "GET test/thing");
      assertEquals(last.getStatusCode(), "201");
      assertEquals(last.getThreadName(), "example 1-2");
      assertFalse(last.isSuccess());
      assertEquals(last.getResponseBytes(), 281L);
      assertEquals(last.getTotalThreads(), 3);

      assertSame(samples.getEarliestSample(), samples.getSamples().getFirst());
      // TODO: Hmm... when multiple samples have the same final timestamp and elapsed time,
      // the first entry wins... should it?
      // assertSame(samples.getLatestSample(), samples.getSamples().getLast());
      assertEquals(samples.getLabels(), List.of("GET test/thing"));
      assertEquals(samples.getThreadNames(), List.of("example 1-1", "example 1-2"));
      assertEquals(samples.getEarliestMillis(), 1469546803634L);
      assertEquals(samples.getLatestMillis(), 1469546804132L);
    }
  }

  @Test
  public void testConvertRowWithMissingColumnSkipped() {
    // If a row is encountered that has a missing column, then skip the defective row.
    String content =
        """
          timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,bytes,allThreads
          1469546803634,496,GET test/thing,200,OK,example 1-1,true,280,2
          1469546803635,496,GET test/thing,200,OK,example 1-1,true,280
          1469546803636,497,GET test/thing,200,OK,example 1-1,true,280,2
          """;
    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvJtlSource.readJtlFile(tc.file());

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
  public void testConvertRowWithExtraColumnSkipped() {
    // If a row is encountered that has an extra column, then skip the defective row.
    String content =
        """
          timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,bytes,allThreads
          1469546803634,496,GET test/thing,200,OK,example 1-1,true,280,2
          1469546803635,496,GET test/thing,200,OK,example 1-1,true,280,2,extra
          1469546803636,497,GET test/thing,200,OK,example 1-1,true,280,2
          """;
    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvJtlSource.readJtlFile(tc.file());

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

  @Test(dataProvider = "nonNumericColumnsDp")
  public void testConvertRowNumericColumnsAreNonNumericAreSkipped(String badRow, String whyBad) {
    String content =
        """
            timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,bytes,allThreads
            1469546803634,496,GET test/thing,200,OK,example 1-1,true,280,2
            %s
            1469546803636,497,GET test/thing,200,OK,example 1-1,true,280,2
            """
            .formatted(badRow);
    // If a row is encountered that should have a numerical value but has a text one, skip it
    try (TempContent tc = TempContent.of(content)) {
      Samples samples = CsvJtlSource.readJtlFile(tc.file());

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

  @DataProvider(name = "nonNumericColumnsDp", parallel = true)
  public static Object[][] nonNumericColoumnsDp() {
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
}
