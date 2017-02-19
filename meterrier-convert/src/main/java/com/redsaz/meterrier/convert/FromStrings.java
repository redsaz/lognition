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

/**
 * Collection of string converters.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
class FromStrings {

    // Don't instanciate Utility Classes.
    private FromStrings() {
    }

    private static class IntegerFromString implements FromString<Integer> {

        public Integer fromString(String str) {
            return Integer.valueOf(str);
        }
    }

    private static class IntegerFromStringOrZero implements FromString<Integer> {

        public Integer fromString(String str) {
            if (str == null) {
                return 0;
            }
            try {
                return Integer.valueOf(str);
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
    }

    private static class LongFromString implements FromString<Long> {

        public Long fromString(String str) {
            return Long.valueOf(str);
        }
    }

    private static class BooleanFromString implements FromString<Boolean> {

        public Boolean fromString(String str) {
            return Boolean.valueOf(str);
        }
    }

    private static class StringFromString implements FromString<String> {

        public String fromString(String str) {
            return str;
        }
    }

    public static final FromString<?> INTEGER_FS = new IntegerFromString();
    public static final FromString<?> INTEGER_FS_OR_ZERO = new IntegerFromStringOrZero();
    public static final FromString<?> LONG_FS = new LongFromString();
    public static final FromString<?> BOOLEAN_FS = new BooleanFromString();
    public static final FromString<?> STRING_FS = new StringFromString();

}
