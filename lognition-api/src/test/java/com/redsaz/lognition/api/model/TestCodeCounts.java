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

import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Redsaz <redsaz@gmail.com>
 */
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
        assertEquals("Incorrect order of codes",
                Arrays.asList("200", "401", "404", "Non-HTTP Status Code: Connection Reset Error"),
                counts.getCodes());

        assertEquals("Incorrect 0th bin code count",
                Arrays.asList(2, 0, 1, 0), counts.getCounts().get(0));
        assertEquals("Incorrect 1st bin code count",
                Arrays.asList(2, 1, 0, 0), counts.getCounts().get(1));
        assertEquals("Incorrect 2nd bin code count",
                Arrays.asList(1, 0, 0, 1), counts.getCounts().get(2));
        assertEquals("Incorrect 3rd bin code count",
                Arrays.asList(0, 0, 0, 0), counts.getCounts().get(3));
        assertEquals("Incorrect 4th bin code count",
                Arrays.asList(1, 0, 0, 0), counts.getCounts().get(4));

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
        assertEquals("Incorrect order of codes",
                Arrays.asList("200", "404"),
                counts.getCodes());

        assertEquals("Incorrect 0th bin code count",
                Arrays.asList(2, 1), counts.getCounts().get(0));

        assertEquals("Incorrect spanMillis", 15000L, counts.getSpanMillis());
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

}
