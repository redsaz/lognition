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

import com.univocity.parsers.annotations.Parsed;

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
     * @param min smallest recorded value
     * @param p25 value at 25th percentile
     * @param p50 value at 59th percentile
     * @param p75 value at 75th percentile
     * @param p90 value at 90th percentile
     * @param p95 value at 95th percentile
     * @param p99 value at 99th percentile
     * @param max largest recored value
     * @param avg mean of all recorded values
     * @param numSamples number of samples that these stats were taken from
     * @param totalResponseBytes total bytes given across all samples
     * @param numErrors number of responses were were in error.
     */
    public Stats(long offsetMillis, Long min, Long p25, Long p50, Long p75, Long p90, Long p95,
            Long p99, Long max, Long avg, long numSamples, long totalResponseBytes, long numErrors) {
        this.offsetMillis = offsetMillis;
        this.min = min;
        this.p25 = p25;
        this.p50 = p50;
        this.p75 = p75;
        this.p90 = p90;
        this.p95 = p95;
        this.p99 = p99;
        this.max = max;
        this.avg = avg;
        this.numSamples = numSamples;
        this.totalResponseBytes = totalResponseBytes;
        this.numErrors = numErrors;
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
