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
package com.redsaz.meterrier.stats;

import com.redsaz.meterrier.api.model.Sample;
import com.univocity.parsers.annotations.Parsed;
import java.util.List;

/**
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class Stats {

    public static final String[] HEADERS = {"offsetMillis", "min", "p25", "p50", "p75", "p90",
        "p95", "p99", "max", "avg", "numSamples", "totalResponseBytes", "numErrors"};

    @Parsed
    private final long offsetMillis;
    @Parsed
    private final Long min;
    @Parsed
    private final Long p25;
    @Parsed
    private final Long p50;
    @Parsed
    private final Long p75;
    @Parsed
    private final Long p90;
    @Parsed
    private final Long p95;
    @Parsed
    private final Long p99;
    @Parsed
    private final Long max;
    @Parsed
    private final Long avg;
    @Parsed
    private final long numSamples;
    @Parsed
    private final long totalResponseBytes;
    @Parsed
    private final long numErrors;

    /**
     * Creates stats based on the provided samples which should already be ordered from shortest
     * duration to longest.
     *
     * @param offsetMillis The point in time, with 0 being the start of the test, that these stats
     * start at
     * @param durationOrderedSamples The samples to calculate the stats on.
     */
    public Stats(long offsetMillis, List<Sample> durationOrderedSamples) {
        this.offsetMillis = offsetMillis;
        if (!durationOrderedSamples.isEmpty()) {
            min = durationOrderedSamples.get(0).getDuration();
            p25 = getElement(durationOrderedSamples, 0.25D).getDuration();
            p50 = getElement(durationOrderedSamples, 0.50D).getDuration();
            p75 = getElement(durationOrderedSamples, 0.75D).getDuration();
            p90 = getElement(durationOrderedSamples, 0.90D).getDuration();
            p95 = getElement(durationOrderedSamples, 0.95D).getDuration();
            p99 = getElement(durationOrderedSamples, 0.99D).getDuration();
            max = durationOrderedSamples.get(durationOrderedSamples.size() - 1).getDuration();
        } else {
            min = null;
            p25 = null;
            p50 = null;
            p75 = null;
            p90 = null;
            p95 = null;
            p99 = null;
            max = null;
        }
        numSamples = durationOrderedSamples.size();
        long cumulativeDuration = 0;
        long cumulativeResponseBytes = 0;
        long cumulativeErrors = 0;
        for (int i = 0; i < durationOrderedSamples.size(); ++i) {
            Sample sample = durationOrderedSamples.get(i);
            cumulativeDuration += sample.getDuration();
            cumulativeResponseBytes += sample.getResponseBytes();
            if (!sample.isSuccess()) {
                ++cumulativeErrors;
            }
        }
        if (numSamples != 0) {
            avg = cumulativeDuration / numSamples;
        } else {
            avg = null;
        }
        totalResponseBytes = cumulativeResponseBytes;
        numErrors = cumulativeErrors;
    }

    private static <T> T getElement(List<T> items, double percent) {
        int index = (int) Math.ceil(((double) (items.size() - 1)) * percent);
        return items.get(index);
    }

    public Long getOffsetMillis() {
        return offsetMillis;
    }

    public Long getMin() {
        return min;
    }

    public Long getP25() {
        return p25;
    }

    public Long getP50() {
        return p50;
    }

    public Long getP75() {
        return p75;
    }

    public Long getP90() {
        return p90;
    }

    public Long getP95() {
        return p95;
    }

    public Long getP99() {
        return p99;
    }

    public Long getMax() {
        return max;
    }

    public Long getAvg() {
        return avg;
    }

    public long getNumSamples() {
        return numSamples;
    }

    public long getTotalResponseBytes() {
        return totalResponseBytes;
    }

    public long getNumErrors() {
        return numErrors;
    }

}
