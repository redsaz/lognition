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
package com.redsaz.meterrier.importer;

import com.redsaz.meterrier.importer.model.jmeter.CsvJtlRow;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Defines columns that can be stored in a JTL CSV. See
 * https://jmeter.apache.org/usermanual/listeners.html
 */
enum JtlType {
    // in milliseconds since 1/1/1970
    TIMESTAMP("timeStamp", FromStrings.LONG_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setTimeStamp((Long) convert(value));
            return dest;
        }
    },
    // in milliseconds
    ELAPSED("elapsed", FromStrings.LONG_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setElapsed((Long) convert(value));
            return dest;
        }
    },
    // sampler label
    LABEL("label", FromStrings.STRING_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setLabel((String) convert(value));
            return dest;
        }
    },
    // e.g. 200, 404
    RESPONSE_CODE("responseCode", FromStrings.STRING_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setResponseCode((String) convert(value));
            return dest;
        }
    },
    // e.g. OK
    RESPONSE_MESSAGE("responseMessage", FromStrings.STRING_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setResponseMessage((String) convert(value));
            return dest;
        }
    },
    // Name of thread
    THREAD_NAME("threadName", FromStrings.STRING_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setThreadName((String) convert(value));
            return dest;
        }
    },
    // e.g. text
    DATA_TYPE("dataType", FromStrings.STRING_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setDataType((String) convert(value));
            return dest;
        }
    },
    // true or false
    SUCCESS("success", FromStrings.BOOLEAN_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setSuccess((Boolean) convert(value));
            return dest;
        }
    },
    // if any
    FAILURE_MESSAGE("failureMessage", FromStrings.STRING_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setFailureMessage((String) convert(value));
            return dest;
        }
    },
    // number of bytes in the sample
    BYTES("bytes", FromStrings.LONG_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setBytes((Long) convert(value));
            return dest;
        }
    },
    // number of bytes sent for the sample
    SENT_BYTES("sentBytes", FromStrings.LONG_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setSentBytes((Long) convert(value));
            return dest;
        }
    },
    // number of active threads in this thread group
    GRP_THREADS("grpThreads", FromStrings.INTEGER_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setGrpThreads((Integer) convert(value));
            return dest;
        }
    },
    // total number of active threads in all groups
    ALL_THREADS("allThreads", FromStrings.INTEGER_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setAllThreads((Integer) convert(value));
            return dest;
        }
    },
    // Uniform Resource Locator
    URL("URL", FromStrings.STRING_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setURL((String) convert(value));
            return dest;
        }
    },
    // If Save Response to File was used
    FILENAME("Filename", FromStrings.STRING_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setFilename((String) convert(value));
            return dest;
        }
    },
    // Time to first response
    LATENCY("Latency", FromStrings.INTEGER_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setLatency((Integer) convert(value));
            return dest;
        }
    },
    // Time to establish connection
    CONNECT("connect", FromStrings.INTEGER_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setConnect((Integer) convert(value));
            return dest;
        }
    },
    ENCODING("encoding", FromStrings.STRING_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setEncoding((String) convert(value));
            return dest;
        }
    },
    // number of samples (1, unless multiple samples are aggregated)
    SAMPLE_COUNT("SampleCount", FromStrings.INTEGER_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setSampleCount((Integer) convert(value));
            return dest;
        }
    },
    // ErrorCount - number of errors (0 or 1, unless multiple samples are aggregated)
    ERROR_COUNT("ErrorCount", FromStrings.INTEGER_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setErrorCount((Integer) convert(value));
            return dest;
        }
    },
    // where the sample was generated
    HOSTNAME("Hostname", FromStrings.STRING_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setHostname((String) convert(value));
            return dest;
        }
    },
    // number of milliseconds of 'Idle' time (normally 0)
    IDLE_TIME("IdleTime", FromStrings.INTEGER_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setIdleTime((Integer) convert(value));
            return dest;
        }
    },
    //if specified
    VARIABLES("Variables", FromStrings.STRING_FS) {
        @Override
        public CsvJtlRow putIn(CsvJtlRow dest, String value) {
            dest.setVariables((String) convert(value));
            return dest;
        }
    };

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

    public abstract CsvJtlRow putIn(CsvJtlRow dest, String value);

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
