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
package com.redsaz.meterrier.services.converter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Defines columns that can be stored in a JTL CSV. See
 * https://jmeter.apache.org/usermanual/listeners.html
 */
enum JtlType {
    TIMESTAMP("timeStamp", FromStrings.LONG_FS), // in milliseconds since 1/1/1970
    ELAPSED("elapsed", FromStrings.LONG_FS), // in milliseconds
    LABEL("label", FromStrings.STRING_FS), // sampler label
    RESPONSE_CODE("responseCode", FromStrings.INTEGER_FS_OR_ZERO), // e.g. 200, 404
    RESPONSE_MESSAGE("responseMessage", FromStrings.STRING_FS), // e.g. OK
    THREAD_NAME("threadName", FromStrings.STRING_FS), // Name of thread
    DATA_TYPE("dataType", FromStrings.STRING_FS), // e.g. text
    SUCCESS("success", FromStrings.BOOLEAN_FS), // true or false
    FAILURE_MESSAGE("failureMessage", FromStrings.STRING_FS), // if any
    BYTES("bytes", FromStrings.LONG_FS), // number of bytes in the sample
    SENT_BYTES("sentBytes", FromStrings.LONG_FS), // number of bytes sent for the sample
    GRP_THREADS("grpThreads", FromStrings.INTEGER_FS), // number of active threads in this thread group
    ALL_THREADS("allThreads", FromStrings.INTEGER_FS), // total number of active threads in all groups
    URL("URL", FromStrings.STRING_FS), // Uniform Resource Locator
    FILENAME("Filename", FromStrings.STRING_FS), // If Save Response to File was used
    LATENCY("Latency", FromStrings.INTEGER_FS), // Time to first response
    CONNECT("connect", FromStrings.INTEGER_FS), // Time to establish connection
    ENCODING("encoding", FromStrings.STRING_FS),
    SAMPLE_COUNT("SampleCount", FromStrings.INTEGER_FS), // number of samples (1, unless multiple samples are aggregated)
    ERROR_COUNT("ErrorCount", FromStrings.INTEGER_FS), // ErrorCount - number of errors (0 or 1, unless multiple samples are aggregated)
    HOSTNAME("Hostname", FromStrings.STRING_FS), // where the sample was generated
    IDLE_TIME("IdleTime", FromStrings.INTEGER_FS), // number of milliseconds of 'Idle' time (normally 0)
    VARIABLES("Variables", FromStrings.STRING_FS);
    //if specified
    private final String csvName;
    private final FromString<?> fromStringer;
    private static final Map<String, JtlType> header2Type = initMap();

    JtlType(String inCsvName, FromString<?> inFromString) {
        csvName = inCsvName;
        fromStringer = inFromString;
    }

    public Object convert(String str) {
        return fromStringer.fromString(str);
    }

    public String csvName() {
        return csvName;
    }

    public static JtlType fromHeader(String header) {
        return header2Type.get(header.toLowerCase(Locale.US));
    }

    private static Map<String, JtlType> initMap() {
        Map<String, JtlType> map = new HashMap<>();
        for (JtlType t : JtlType.values()) {
            map.put(t.csvName.toLowerCase(Locale.US), t);
        }
        return map;
    }
}
