/*
 * Copyright 2020 Redsaz <redsaz@gmail.com>.
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
package com.redsaz.lognition.api.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** @author Redsaz <redsaz@gmail.com> */
public class TestCodeCounts {

  @Test
  public void testBuilder() {
    CodeCounts.Builder builder = new CodeCounts.Builder(15000L);
    // Counts for 0ms-14999ms
    builder.increment("200");
    builder.increment("200");
    builder.increment("404");
    builder.commitBin();

    // Counts for 15000ms-29999ms
    builder.increment("200");
    builder.increment("401");
    builder.increment("200");
    builder.commitBin();

    // Counts for 30000ms-44999ms
    builder.increment("Non-HTTP Status Code: Connection Reset Error");
    builder.increment("200");
    builder.commitBin();

    // Counts for 45000ms-59999ms
    // Intentionally left empty
    builder.commitBin();

    // Counts for 60000ms-74999ms
    builder.increment("200");
    builder.commitBin();

    CodeCounts counts = builder.build();

    // There should be 5 bins, 4 codes in each.
    assertEquals("Incorrect number of bins", 5, counts.getCounts().size());
    assertEquals(
        "Incorrect order of codes",
        Arrays.asList("200", "401", "404", "Non-HTTP Status Code: Connection Reset Error"),
        counts.getCodes());

    assertEquals(
        "Incorrect 0th bin code count", Arrays.asList(2, 0, 1, 0), counts.getCounts().get(0));
    assertEquals(
        "Incorrect 1st bin code count", Arrays.asList(2, 1, 0, 0), counts.getCounts().get(1));
    assertEquals(
        "Incorrect 2nd bin code count", Arrays.asList(1, 0, 0, 1), counts.getCounts().get(2));
    assertEquals(
        "Incorrect 3rd bin code count", Arrays.asList(0, 0, 0, 0), counts.getCounts().get(3));
    assertEquals(
        "Incorrect 4th bin code count", Arrays.asList(1, 0, 0, 0), counts.getCounts().get(4));

    assertEquals("Incorrect spanMillis", 15000L, counts.getSpanMillis());
  }

  @Test
  public void testBuilder_oneBin() {
    CodeCounts.Builder builder = new CodeCounts.Builder(15000L);
    // Counts for 0ms-14999ms
    builder.increment("200");
    builder.increment("200");
    builder.increment("404");
    builder.commitBin();

    CodeCounts counts = builder.build();

    // There should be 1 bin, 2 codes.
    assertEquals("Incorrect number of bins", 1, counts.getCounts().size());
    assertEquals("Incorrect order of codes", Arrays.asList("200", "404"), counts.getCodes());

    assertEquals("Incorrect 0th bin code count", Arrays.asList(2, 1), counts.getCounts().get(0));

    assertEquals("Incorrect spanMillis", 15000L, counts.getSpanMillis());
  }

  @Test
  public void testBuilder_unorderedInput_orderedCodes() {
    CodeCounts.Builder builder = new CodeCounts.Builder(15000L);
    // Given codes received in a random order,
    // (Counts for 0ms-14999ms)
    // (The final ordering is by alphabetical order of the codes, not by the count of the codes.
    // A unique count was needed for each code, and having them ultimately order from 1-7 was
    // an easy way to visually see that the ordering is correct when viewed in a debugger)
    builder.increment("204");
    builder.increment("204");
    builder.increment("500");
    builder.increment("500");
    builder.increment("500");
    builder.increment("500");
    builder.increment("500");
    builder.increment("404");
    builder.increment("404");
    builder.increment("404");
    builder.increment("404");
    builder.increment("Non-HTTP Status Code: Connection Reset Error");
    builder.increment("Non-HTTP Status Code: Connection Reset Error");
    builder.increment("Non-HTTP Status Code: Connection Reset Error");
    builder.increment("Non-HTTP Status Code: Connection Reset Error");
    builder.increment("Non-HTTP Status Code: Connection Reset Error");
    builder.increment("Non-HTTP Status Code: Connection Reset Error");
    builder.increment("Non-HTTP Status Code: Connection Reset Error");
    builder.increment("401");
    builder.increment("401");
    builder.increment("401");
    builder.increment("503");
    builder.increment("503");
    builder.increment("503");
    builder.increment("503");
    builder.increment("503");
    builder.increment("503");
    builder.increment("200");
    builder.commitBin();

    // When I build the code counts,
    CodeCounts counts = builder.build();

    // Then the code counts should be ordered alphabetically,
    assertEquals(
        "Incorrect order of codes",
        Arrays.asList(
            "200",
            "204",
            "401",
            "404",
            "500",
            "503",
            "Non-HTTP Status Code: Connection Reset Error"),
        counts.getCodes());
    // and should have the right counts
    assertEquals(
        "Incorrect order of codes", Arrays.asList(1, 2, 3, 4, 5, 6, 7), counts.getCounts().get(0));
  }

  @Test
  public void testBuilder_zeroBins() {
    CodeCounts.Builder builder = new CodeCounts.Builder(15000L);
    // commitBin never called.

    CodeCounts counts = builder.build();

    // There should be 0 bins, 0 codes.
    assertEquals("Incorrect number of bins", 0, counts.getCounts().size());
    assertEquals("Incorrect number of codes", 0, counts.getCodes().size());

    assertEquals("Incorrect spanMillis", 15000L, counts.getSpanMillis());
  }

  @Test
  public void testBuilder_commitBinNotCalled_noBins() {
    CodeCounts.Builder builder = new CodeCounts.Builder(15000L);

    // If the user adds codes, but doesn't commit the bin,
    // (Counts for 0ms-14999ms)
    builder.increment("200");
    builder.increment("200");
    builder.increment("404");
    // (commitBin not called)

    CodeCounts counts = builder.build();

    // Then there should be 0 bins, 0 codes, because the bin only counts if it is committed.
    assertEquals("Incorrect number of bins", 0, counts.getCounts().size());
    assertEquals("Incorrect number of codes", 0, counts.getCodes().size());

    assertEquals("Incorrect spanMillis", 15000L, counts.getSpanMillis());
  }

  @Test(expected = NullPointerException.class)
  public void testBuilder_nullCode_throwException() {
    // Given a CodeCounts Builder,
    CodeCounts.Builder builder = new CodeCounts.Builder(15000L);

    // When a null code is incremented,
    builder.increment(null);

    // Then a NullPointerException is thrown. (Verified by test harness)
  }

  @Test(expected = NullPointerException.class)
  public void testBuilder_emptyCode_throwException() {
    // Given a CodeCounts Builder,
    CodeCounts.Builder builder = new CodeCounts.Builder(15000L);

    // When an empty code is incremented,
    builder.increment("");

    // Then a NullPointerException is thrown. (Verified by test harness)
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testCodeCounts_immutableBin() {
    // Given lists of codes and bins of code counts,
    List<String> codeList = Arrays.asList("200");
    List<List<Integer>> codeCounts = Arrays.asList(Arrays.asList(4));

    // When I create a CodeCounts object with those lists,
    CodeCounts counts = new CodeCounts(15000L, codeList, codeCounts);

    // Then altering a bin from the orginal shouldn't change the bin in the CodeCounts,
    codeCounts.get(0).set(0, 9001);
    assertEquals(4, counts.getCounts().get(0).get(0).intValue());

    // and trying to alter a bin in codecounts should throw exception.
    // (Verified by test harness)
    counts.getCounts().get(0).set(0, 9001);
    assertEquals(4, counts.getCounts().get(0).get(0).intValue());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testCodeCounts_immutableBinList() {
    // Given lists of codes and bins of code counts,
    List<String> codeList = Arrays.asList("200");
    List<List<Integer>> codeCounts = Arrays.asList(Arrays.asList(4));

    // When I create a CodeCounts object with those lists,
    CodeCounts counts = new CodeCounts(15000L, codeList, codeCounts);

    // Then trying to alter the outer list should result in an exception.
    // (Verified by test harness)
    counts.getCounts().set(0, Arrays.asList(9001));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testCodeCounts_immutableCounts() {
    // Given lists of codes and bins of code counts,
    List<String> codeList = Arrays.asList("200");
    List<List<Integer>> codeCounts = Arrays.asList(Arrays.asList(4));

    // When I create a CodeCounts object with those lists,
    CodeCounts counts = new CodeCounts(15000L, codeList, codeCounts);

    // Then altering the original list of codes shouldn't change the data in the CodeCounts,
    codeList.set(0, "BOGUS");
    assertEquals("200", counts.getCodes().get(0));

    // and trying to alter the codeCounts list should result in an exception.
    // (Verified by test harness)
    counts.getCodes().set(0, "BOGUS");
  }

  /**
   * This tests calling normalizeUsing() in the usual scenario: Each log usually has different
   * sample labels and one "overall" label. The "overall" label will have code counts that are the
   * sum of all the code counts of the sample labels, so it will actually have all of the codes that
   * we care about.
   *
   * <p>"Normalizing" in this case means filling in the missing codes with a count of 0, so that
   * they can be compared in a table.
   */
  @Test
  public void testNormalizeUsing() {
    // Given one CodeCounts with counts for 200 and 404,
    CodeCounts label1 =
        new CodeCounts.Builder(15000L)
            .increment("200")
            .increment("200")
            .increment("400")
            .commitBin()
            .build();

    // and another with counts for 200, 301, and 404,
    CodeCounts overall =
        new CodeCounts.Builder(15000L)
            .increment("200")
            .increment("200")
            .increment("400")
            .increment("301")
            .commitBin()
            .build();

    // When I normalize the first using the codes from the second CodeCounts,
    CodeCounts actual = label1.normalizeUsing(overall.getCodes());

    // Then the result should be the same as the original, but with new code "301" with count 0.
    assertEquals("spanMillis shouldn't change", label1.getSpanMillis(), actual.getSpanMillis());
    assertEquals(
        "count for 200 shouldn't change",
        label1.getCounts().get(0).get(0),
        actual.getCounts().get(0).get(0));
    assertEquals("count for 301 should be 0", Integer.valueOf(0), actual.getCounts().get(0).get(1));
    assertEquals(
        "count for 400 shouldn't change",
        label1.getCounts().get(0).get(1),
        actual.getCounts().get(0).get(2));
  }

  @Test
  public void testNormalizeUsing_emptyCodesList() {
    // Given one CodeCounts with counts for 200 and 404,
    CodeCounts label1 =
        new CodeCounts.Builder(15000L)
            .increment("200")
            .increment("200")
            .increment("400")
            .commitBin()
            .build();

    // When I normalize it with an empty codes list,
    CodeCounts actual = label1.normalizeUsing(Collections.emptyList());

    // Then the result should be an empty CodesList, without bins or codes.
    assertEquals("Should have no codes", Collections.emptyList(), actual.getCodes());
    assertEquals("Should have no counts", Collections.emptyList(), actual.getCounts());
  }

  @Test
  public void testNormalizeUsing_emptyOriginal() {
    // Given one empty CodeCounts, without bins or codes,
    CodeCounts label1 = new CodeCounts.Builder(15000L).build();

    // When I normalize it to a codes list,
    CodeCounts actual = label1.normalizeUsing(Arrays.asList("200"));

    // Then the result should be an empty CodesList, without bins or codes.
    assertEquals("Should have no codes", Collections.emptyList(), actual.getCodes());
    assertEquals("Should have no counts", Collections.emptyList(), actual.getCounts());
  }

  @Test
  public void testNormalizeUsing_equalCodes() {
    // Given one CodeCounts with counts for 200 and 404,
    CodeCounts label1 =
        new CodeCounts.Builder(15000L)
            .increment("200")
            .increment("200")
            .increment("400")
            .commitBin()
            .build();

    // When I normalize it with an equal codes list,
    CodeCounts actual = label1.normalizeUsing(Arrays.asList("200", "400"));

    // Then the result should be the same instance as the original
    assertSame(
        "The same CodeCounts should be returned if the code lists are the same.", label1, actual);
  }
}
