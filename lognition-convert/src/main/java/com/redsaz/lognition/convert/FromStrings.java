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
package com.redsaz.lognition.convert;

/**
 * Collection of string converters.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
/*package protected*/ class FromStrings {

    // Don't instanciate Utility Classes.
    private FromStrings() {
    }

    public static final FromString<Integer> INTEGER_FS = (String str) -> Integer.valueOf(str);

    public static final FromString<Integer> INTEGER_FS_OR_ZERO = (String str) -> {
        if (str == null) {
            return 0;
        }
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException ex) {
            return 0;
        }
    };

    public static final FromString<Long> LONG_FS = (String str) -> Long.valueOf(str);

    public static final FromString<Boolean> BOOLEAN_FS = (String str) -> Boolean.valueOf(str);

    public static final FromString<String> STRING_FS = (String str) -> str;

}
