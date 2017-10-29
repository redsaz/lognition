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
package com.redsaz.meterrier.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Redsaz <redsaz@gmail.com>
 */
public class Percentiles {

    private final List<Long> totalCounts;
    private final List<Long> values;
    private final List<Double> percentiles;

    /**
     * Create a new percentiles list.
     *
     * @param totalCounts array of the number of samples covered by the percentiles. Must be same
     * number of elements as values and bucketMaximums arrays.
     * @param values array of the value at each percentile. Must be same size as percentiles.
     * @param percentiles array of percentiles
     */
    @JsonCreator
    public Percentiles(
            @JsonProperty("totalCounts") List<Long> totalCounts,
            @JsonProperty("values") List<Long> values,
            @JsonProperty("percentiles") List<Double> percentiles) {
        if (totalCounts == null || values == null || percentiles == null
                || totalCounts.size() != percentiles.size() && totalCounts.size() != values.size()) {
            throw new IllegalArgumentException("totalCounts, values, and percentiles cannot be null and must be equal in size.");
        }
        this.totalCounts = Collections.unmodifiableList(new ArrayList<>(totalCounts));
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
        this.percentiles = Collections.unmodifiableList(new ArrayList<>(percentiles));
    }

    public List<Long> getCounts() {
        return totalCounts;
    }

    public List<Long> getValues() {
        return values;
    }

    public List<Double> getPercentiles() {
        return percentiles;
    }

    @JsonIgnore
    public int size() {
        return totalCounts.size();
    }
}
