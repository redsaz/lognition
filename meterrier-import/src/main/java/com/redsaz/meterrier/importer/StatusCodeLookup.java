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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines a condensed form of all standard status codes. These are so
 * common that they can be hard0coded. For the weird web servers that do not
 * follow these standard response codes, they will be added to a custom
 * lookup. This mapping will need stored along with the sample data in order
 * to be successfully retrieved.
 *
 * This takes advantage of compact integer encoders for avro or protobuf
 * that use zig-zag variable-length integers. The normal status codes all
 * have reference values of 0 to 63, and custom status codes go from -1 to
 * -63 (and further, if need be). This ensures that the status code will fit
 * in a single byte, instead of 4 like a normal integer would be (otherwise
 * we're actually WASTING a byte).
 */
class StatusCodeLookup {

    private static final List<Integer> codeToRef = new ArrayList<>();
    private static final List<Integer> refToCode;
    // What reference to start counting the customer status codes from.
    private static final int CUSTOM_START_REF = -1;
    // How many integers to skip for each new ref. Negative means going down.
    private static final int CUSTOM_SPAN = -1;
    // Key=code Value=Ref
    private Map<Integer, Integer> customLookup = new HashMap<>();
    private List<Integer> customCodes = new ArrayList<>();
    static {
        refToCode = Arrays.asList(0, 100, 101, 102, 200, 201, 202, 203, 204, 205, 206, 207, 208, 226, 300, 301, 302, 303, 304, 305, 307, 308, 400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 421, 422, 423, 424, 426, 428, 429, 431, 444, 451, 499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 510, 511, 599);
        int ref = 0;
        int codeSize = refToCode.get(refToCode.size() - 1) + 1;
        for (int i = 0; i < codeSize; ++i) {
            codeToRef.add(null);
        }
        for (Integer code : refToCode) {
            codeToRef.set(code, ref);
            ++ref;
        }
    }

    public StatusCodeLookup() {
    }

    public Integer getRef(Integer code) {
        if (code == null) {
            return null;
        }
        Integer ref = null;
        if (0 <= code && code <= codeToRef.size()) {
            ref = codeToRef.get(code);
        }
        if (ref == null) {
            ref = customLookup.get(code);
            if (ref == null) {
                ref = CUSTOM_START_REF + (customCodes.size() * CUSTOM_SPAN);
                customLookup.put(code, ref);
                customCodes.add(code);
            }
        }
        return ref;
    }

    public Integer getCode(Integer ref) {
        if (ref == null) {
            return null;
        }
        if (0 <= ref && ref <= refToCode.size()) {
            return refToCode.get(ref);
        }
        return customCodes.get((ref - CUSTOM_START_REF) / CUSTOM_SPAN);
    }

}
