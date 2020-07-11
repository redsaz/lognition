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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Stores an immutable count of how many times different status codes appear, grouped by time
 * slices.
 * <p>
 * Though it is possible to construct it, a builder can be used while reading a log to collect the
 * counts. The builder itself is not thread safe.
 * <p>
 * This structure can be used for both aggregate data (a count of codes across the entire run) and
 * timeseries data (a count of codes, grouped into time slices). The way to do the aggregate form is
 * to specify a timespan of the length of the entire run, and have a single bin of all the counts.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CodeCounts {

    private final long spanMs;
    private final List<String> codes;
    // int[bins][codes]
    private final List<List<Integer>> counts;

    public CodeCounts(long spanMillis, List<String> codeList, List<List<Integer>> codeCounts) {
        spanMs = spanMillis;
        codes = Collections.unmodifiableList(new ArrayList<>(codeList));
        counts = twoDCopy(codeCounts);
    }

    public long getSpanMillis() {
        return spanMs;
    }

    public List<String> getCodes() {
        return codes;
    }

    public List<List<Integer>> getCounts() {
        return counts;
    }

    private static List<List<Integer>> twoDCopy(List<List<Integer>> arry) {
        List<List<Integer>> result = new ArrayList<>(arry.size());

        arry.stream()
                .forEachOrdered(c -> result.add(
                Collections.unmodifiableList(new ArrayList<>(c))
        ));

        return Collections.unmodifiableList(result);
    }

    public static class Builder {

        private final long spanMs;
        private final Set<String> codes = new HashSet<>();
        private Map<String, Integer> currentBin = new HashMap<>();
        private final List<Map<String, Integer>> binsOfCodeCounts
                = new ArrayList<>(Collections.singleton(currentBin));

        public Builder(long spanMillis) {
            spanMs = spanMillis;
        }

        public void increment(String code) {
            if (Objects.requireNonNull(code).isEmpty()) {
                throw new NullPointerException("Code cannot be null or empty.");
            }

            codes.add(code);
            currentBin.merge(code, 1, (oldValue, u) -> oldValue + 1);
        }

        public void nextBin() {
            codes.addAll(currentBin.keySet());
            currentBin = new HashMap<>();
            binsOfCodeCounts.add(currentBin);
        }

        public CodeCounts build() {
            List<String> orderedCodes = new ArrayList<>(codes);
            Map<String, Integer> lookup = new HashMap<>();
            for (int i = 0; i < orderedCodes.size(); ++i) {
                lookup.put(orderedCodes.get(i), i);
            }

            List<List<Integer>> bins = new ArrayList<>(binsOfCodeCounts.size());
            for (int i = 0; i < binsOfCodeCounts.size(); ++i) {
                // Create a new bin of code counts, with all counts initialized to 0.
                ArrayList<Integer> binCodes
                        = new ArrayList<Integer>(Collections.nCopies(orderedCodes.size(), 0));
                bins.add(binCodes);
                // Insert each count into the position of the list for the code.
                binsOfCodeCounts.get(i).entrySet().stream()
                        .forEachOrdered(e -> {
                            int codeIndex = lookup.get(e.getKey());
                            binCodes.set(codeIndex, e.getValue());
                        });
            }

            return new CodeCounts(spanMs, orderedCodes, bins);
        }
    }
}
