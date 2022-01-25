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
package com.redsaz.lognition.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains distributions of buckets of values. Each bucket in the histogram is a count of the items
 * in that bucket, and the range is between the previous bucket's max (exclusive) and the current
 * bucket's max. The first bucket, which has no previous bucket, has a minimum of 0 inclusive.
 *
 * <p>For example: <br>
 * {@code Histogram hist = new Histogram(Arrays.asList(20,32,43,31), Arrays.asList(1,3,5,7))}<br>
 * This histogram has 4 buckets:
 *
 * <table>
 * <tr><th>Min (inclusive)</th><th>Max (inclusive)</th><th>Count</th></tr>
 * <tr><td>0</td><td>1</td><td>20</td></tr>
 * <tr><td>2</td><td>3</td><td>32</td></tr>
 * <tr><td>4</td><td>5</td><td>43</td></tr>
 * <tr><td>6</td><td>7</td><td>31</td></tr>
 * </table>
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class Histogram {

  private final List<Long> counts;
  private final List<Long> bucketMaximums;

  /**
   * Create a new histogram.
   *
   * @param counts array of all the counts for each bucket. Must be same size as bucketMaximums
   * @param bucketMaximums array of the max (inclusive) value for each bucket.
   */
  @JsonCreator
  public Histogram(
      @JsonProperty("counts") List<Long> counts,
      @JsonProperty("bucketMaximums") List<Long> bucketMaximums) {
    if (counts == null || bucketMaximums == null || counts.size() != bucketMaximums.size()) {
      throw new IllegalArgumentException(
          "counts and bucketMaximums cannot be null and must be equal in size.");
    }
    this.counts = Collections.unmodifiableList(new ArrayList<>(counts));
    this.bucketMaximums = Collections.unmodifiableList(new ArrayList<>(bucketMaximums));
  }

  public List<Long> getCounts() {
    return counts;
  }

  public List<Long> getBucketMaximums() {
    return bucketMaximums;
  }

  @JsonIgnore
  public int size() {
    return counts.size();
  }

  @JsonIgnore
  public long bucketMinInclusive(int index) {
    if (index == 0) {
      return 0;
    }
    return bucketMaximums.get(index - 1);
  }

  @JsonIgnore
  public long bucketMaxInclusive(int index) {
    return bucketMaximums.get(index);
  }

  @JsonIgnore
  public long bucketCount(int index) {
    return counts.get(index);
  }
}
