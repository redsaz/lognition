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
 *
 * <p>Though it is possible to construct it, a builder can be used while reading a log to collect
 * the counts. The builder itself is not thread safe.
 *
 * <p>This structure can be used for both aggregate data (a count of codes across the entire run)
 * and timeseries data (a count of codes, grouped into time slices). The way to do the aggregate
 * form is to specify a timespan of the length of the entire run, and have a single bin of all the
 * counts.
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

  /**
   * Given a list of codes, will return a new CodeCounts that contains only those codes, in that
   * order. Any codes that was included in the original CodeCounts but weren't listed in the
   * codeList will be dropped. Any codes not included in the original but are listed in the codeList
   * will be added with a count of 0.
   *
   * <p>Example: If CodeCounts x contains code->count of [200->3,301->4,404->5], and we call
   * x.normalizeUsing(Arrays.of("200", "204", "404")), then a new CodeCounts is returned with:
   * [200->3, 204->0, 404->5]. Note that the new CodeCounts is missing the "301" we had earlier, and
   * picked up a "204" code with amount of 0.
   *
   * @param codeList the list of codes to normalize to
   * @return A new CodeCounts containing the codes from the codeList, in the order of that list.
   */
  public CodeCounts normalizeUsing(List<String> codeList) {
    if (codeList.equals(codes)) {
      return this;
    }
    // If the new code list is empty, then return an empty CodeCounts.
    // Or, if our CodeCounts is empty (no codes and no bins), then return an empty CodeCounts.
    if (codeList.isEmpty() || (codes.isEmpty() && counts.isEmpty())) {
      return new CodeCounts(this.getSpanMillis(), Collections.emptyList(), Collections.emptyList());
    }

    // Since counts are stored [bin][counts], this means we're rearranging the counts based on
    // the new positions of the codes (it is stored in lexicographical order), and must repeat
    // that for every bin.
    // (If it was stored [counts][bin], then it'd be a matter of finding the index and grabbing
    // the bin list once per code, but this is not the case here.)
    //
    // The algorithm is to go through every code in the current CodeCount, and find its position
    // in the given codeList (or -1 if not in the given codeList). These are put into a list in
    // the order of the current CodeCount.
    // So if the original index is 0, but the new is 1, then newIdx[0] == 1. Or, if the
    // original index was 1, but the new counts won't have that code, then newIdx[1] == -1.
    List<Integer> newIdx = new ArrayList<>(codes.size());
    for (String code : codes) {
      int newPos = codeList.indexOf(code);

      newIdx.add(newPos);
    }

    // Next, for each bin, we create a new 0-filled list, and copy the values from the original
    // list for the bin into the new one.
    int newSize = codeList.size();
    List<List<Integer>> newCounts = new ArrayList<>(counts.size());
    for (List<Integer> bin : counts) {
      List<Integer> newBin = new ArrayList<>(Collections.nCopies(newSize, 0));

      for (int i = 0; i < newIdx.size(); ++i) {
        int newPos = newIdx.get(i);
        if (newPos >= 0) {
          newBin.set(newPos, bin.get(i));
        }
      }

      newCounts.add(newBin);
    }

    return new CodeCounts(spanMs, codeList, newCounts);
  }

  private static List<List<Integer>> twoDCopy(List<List<Integer>> arry) {
    List<List<Integer>> result = new ArrayList<>(arry.size());

    arry.stream().forEachOrdered(c -> result.add(Collections.unmodifiableList(new ArrayList<>(c))));

    return Collections.unmodifiableList(result);
  }

  public static class Builder {

    private final long spanMs;
    private final Set<String> codes = new HashSet<>();
    private Map<String, Integer> currentBin = new HashMap<>();
    private final List<Map<String, Integer>> binsOfCodeCounts = new ArrayList<>();

    public Builder(long spanMillis) {
      spanMs = spanMillis;
    }

    public Builder increment(String code) {
      if (Objects.requireNonNull(code).isEmpty()) {
        throw new NullPointerException("Code cannot be null or empty.");
      }

      currentBin.merge(code, 1, (oldValue, u) -> oldValue + 1);

      return this;
    }

    /** Commits the current bin of counts and prepares a new bin of counts. */
    public Builder commitBin() {
      codes.addAll(currentBin.keySet());
      binsOfCodeCounts.add(currentBin);
      currentBin = new HashMap<>();

      return this;
    }

    public CodeCounts build() {
      List<String> orderedCodes = new ArrayList<>(codes);
      Collections.sort(orderedCodes);
      Map<String, Integer> lookup = new HashMap<>();
      for (int i = 0; i < orderedCodes.size(); ++i) {
        lookup.put(orderedCodes.get(i), i);
      }

      List<List<Integer>> bins = new ArrayList<>(binsOfCodeCounts.size());
      for (int i = 0; i < binsOfCodeCounts.size(); ++i) {
        // Create a new bin of code counts, with all counts initialized to 0.
        ArrayList<Integer> binCodes =
            new ArrayList<Integer>(Collections.nCopies(orderedCodes.size(), 0));
        bins.add(binCodes);
        // Insert each count into the position of the list for the code.
        binsOfCodeCounts.get(i).entrySet().stream()
            .forEachOrdered(
                e -> {
                  int codeIndex = lookup.get(e.getKey());
                  binCodes.set(codeIndex, e.getValue());
                });
      }

      return new CodeCounts(spanMs, orderedCodes, bins);
    }
  }
}
