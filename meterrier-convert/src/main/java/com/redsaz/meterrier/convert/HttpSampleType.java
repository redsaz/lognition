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
package com.redsaz.meterrier.convert;

import com.redsaz.meterrier.convert.model.HttpSample;

/**
 * Defines columns that can be stored in a generic http sample log, and the
 * Jmeter types to convert from.
 */
enum HttpSampleType {
    MILLIS_OFFSET(JtlType.TIMESTAMP), // in milliseconds the previous sample, or millis since epoch if first entry.
    MILLIS_ELAPSED(JtlType.ELAPSED), // in milliseconds
    LABEL_REF(JtlType.LABEL), // sampler label
    RESPONSE_CODE(JtlType.RESPONSE_CODE), // e.g. 200, 404
    SUCCESS(JtlType.SUCCESS), // true if the response code was the intented response
    BYTES_RECEIVED(JtlType.BYTES), // number of bytes in the sample
    BYTES_SENT(JtlType.SENT_BYTES), // number of bytes sent for the sample
    CURRENT_THREADS(JtlType.ALL_THREADS), // number of threads available for running scripts
    URL_REF(JtlType.URL);
    // Uniform Resource Locator
    private final JtlType jtlType;

    HttpSampleType(JtlType inJtlType) {
        jtlType = inJtlType;
    }

    /**
     * Modifies the given sample with the value, the modification determined by
     * the enum.
     *
     * @param sample target to modify
     * @param value value to put into target.
     */
    public void modify(HttpSample sample, String value) {
    }

}
