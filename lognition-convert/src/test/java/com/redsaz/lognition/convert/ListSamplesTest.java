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
import static org.testng.Assert.assertNull;

import com.redsaz.lognition.api.model.Sample;
import java.io.IOException;
import java.util.List;
import org.testng.annotations.Test;

public class ListSamplesTest {

  @Test
  public void testBuild() throws IOException {
    // When a list of samples is built into ListSamples,
    Samples samples =
        ListSamples.builder()
            .add(Sample.of(1254L, 10L, "GET example/{id}", "1", "200", "OK", true, 123L, 2))
            .add(
                Sample.of(
                    1367L,
                    12L,
                    "GET fail/{id}",
                    "1",
                    "NonHttpStatusCode",
                    "Connection Refused",
                    false,
                    123L,
                    2))
            .add(Sample.of(1607L, 20L, "GET example/{id}", "2", "200", "OK", true, 123L, 2))
            .build();

    // Then the ListSamples is in order by offset,
    // and offsets are normalized: re-zeroed relative to the earliesst provided sample.
    List<Sample> expecteds =
        List.of(
            Sample.of(0L, 10L, "GET example/{id}", "1", "200", "OK", true, 123L, 2),
            Sample.of(
                113L,
                12L,
                "GET fail/{id}",
                "1",
                "NonHttpStatusCode",
                "Connection Refused",
                false,
                123L,
                2),
            Sample.of(353L, 20L, "GET example/{id}", "2", "200", "OK", true, 123L, 2));
    assertEquals(samples.getSamples(), expecteds);
    assertEquals(samples.getEarliestSample(), expecteds.getFirst());
    assertEquals(samples.getLatestSample(), expecteds.getLast());
    assertEquals(samples.getLabels(), List.of("GET example/{id}", "GET fail/{id}"));
    assertEquals(samples.getThreadNames(), List.of("1", "2"));
    assertEquals(
        samples.getEarliestMillis(),
        1254L,
        "Earliest should be timestamp from epoch when the earliest start happened.");
    assertEquals(
        samples.getLatestMillis(),
        1627L,
        "Latest should be timestamp from epoch when the latest finish happened (start of sample plus duration).");
  }

  @Test
  public void testBuildUnordered() throws IOException {
    // When a list of unordered samples is built into ListSamples,
    Samples samples =
        ListSamples.builder()
            .add(Sample.of(1607L, 20L, "GET example/{id}", "2", "200", "OK", true, 123L, 2))
            .add(
                Sample.of(
                    1367L,
                    12L,
                    "GET fail/{id}",
                    "1",
                    "NonHttpStatusCode",
                    "Connection Refused",
                    false,
                    123L,
                    2))
            .add(Sample.of(1254L, 10L, "GET example/{id}", "1", "200", "OK", true, 123L, 2))
            .build();

    // Then the ListSamples is in order by offset,
    // and offsets are normalized: re-zeroed relative to the earliesst provided sample.
    List<Sample> expecteds =
        List.of(
            Sample.of(0L, 10L, "GET example/{id}", "1", "200", "OK", true, 123L, 2),
            Sample.of(
                113L,
                12L,
                "GET fail/{id}",
                "1",
                "NonHttpStatusCode",
                "Connection Refused",
                false,
                123L,
                2),
            Sample.of(353L, 20L, "GET example/{id}", "2", "200", "OK", true, 123L, 2));
    assertEquals(samples.getSamples(), expecteds);
    assertEquals(samples.getEarliestSample(), expecteds.getFirst());
    assertEquals(samples.getLatestSample(), expecteds.getLast());
    assertEquals(samples.getLabels(), List.of("GET example/{id}", "GET fail/{id}"));
    assertEquals(samples.getThreadNames(), List.of("1", "2"));
    assertEquals(
        samples.getEarliestMillis(),
        1254L,
        "Earliest should be timestamp from epoch when the earliest start happened.");
    assertEquals(
        samples.getLatestMillis(),
        1627L,
        "Latest should be timestamp from epoch when the latest finish happened (start of sample plus duration).");
  }

  @Test
  public void testBuildOneSample() {
    // When a list size of one sample is built into ListSamples,
    Samples samples =
        ListSamples.builder()
            .add(Sample.of(1254L, 10L, "GET example/{id}", "1", "200", "OK", true, 123L, 2))
            .build();

    // Then it should result in a ListSamples of size 1
    List<Sample> expecteds =
        List.of(Sample.of(0L, 10L, "GET example/{id}", "1", "200", "OK", true, 123L, 2));
    assertEquals(samples.getSamples(), expecteds);
    assertEquals(samples.getEarliestSample(), expecteds.getFirst());
    assertEquals(samples.getLatestSample(), expecteds.getLast());
    assertEquals(samples.getLabels(), List.of("GET example/{id}"));
    assertEquals(samples.getThreadNames(), List.of("1"));
    assertEquals(
        samples.getEarliestMillis(),
        1254L,
        "Earliest should be timestamp from epoch when the earliest start happened.");
    assertEquals(
        samples.getLatestMillis(),
        1264L,
        "Latest should be timestamp from epoch when the latest finish happened (start of sample plus duration).");
  }

  @Test
  public void testBuildZeroSamples() {
    // When a list size of zero samples is built into ListSamples,
    Samples samples = ListSamples.builder().build();

    // Then it should result in a ListSamples of size 0
    List<Sample> expecteds = List.of();
    assertEquals(samples.getSamples(), expecteds);
    assertNull(samples.getEarliestSample());
    assertNull(samples.getLatestSample());
    assertEquals(samples.getLabels(), List.of());
    assertEquals(samples.getThreadNames(), List.of());
    assertEquals(samples.getEarliestMillis(), 0L);
    assertEquals(samples.getLatestMillis(), 0L);
  }
}
