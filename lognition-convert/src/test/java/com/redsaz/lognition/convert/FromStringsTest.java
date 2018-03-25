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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the FromStrings class.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class FromStringsTest {

    @Test(dataProvider = "goodIntegers")
    public void testIntegerFromString(String input, Integer expected) {
        Integer actual = FromStrings.INTEGER_FS.fromString(input);
        assertEquals(actual, expected);
    }

    @Test(dataProvider = "badIntegers", expectedExceptions = {NumberFormatException.class})
    public void testIntegerFromStringBadInts(String input) {
        FromStrings.INTEGER_FS.fromString(input);
        fail("Should not have gotten to this point.");
    }

    @Test(dataProvider = "goodIntegers")
    public void testIntegerFromStringOrZero(String input, Integer expected) {
        Integer actual = FromStrings.INTEGER_FS_OR_ZERO.fromString(input);
        assertEquals(actual, expected);
    }

    @Test(dataProvider = "badIntegers")
    public void testIntegerFromStringOrZeroBadInts(String input) {
        Integer actual = FromStrings.INTEGER_FS_OR_ZERO.fromString(input);
        assertEquals(actual.intValue(), 0);
    }

    @Test(dataProvider = "goodLongs")
    public void testLongFromString(String input, Long expected) {
        Long actual = FromStrings.LONG_FS.fromString(input);
        assertEquals(actual, expected);
    }

    @Test(dataProvider = "badLongs", expectedExceptions = {NumberFormatException.class})
    public void testLongFromStringBadLongs(String input) {
        FromStrings.LONG_FS.fromString(input);
        fail("Should not have gotten to this point.");
    }

    @Test(dataProvider = "goodBooleans")
    public void testBooleanFromString(String input, Boolean expected) {
        Boolean actual = FromStrings.BOOLEAN_FS.fromString(input);
        assertEquals(actual, expected);
    }

    @Test(dataProvider = "badBooleans")
    public void testBooleanFromStringBadBooleans(String input) {
        Boolean actual = FromStrings.BOOLEAN_FS.fromString(input);
        assertFalse(actual);
    }

    @Test(dataProvider = "goodStrings")
    public void testStringFromString(String input) {
        String actual = FromStrings.STRING_FS.fromString(input);
        assertSame(actual, input);
    }

    @DataProvider(name = "goodIntegers", parallel = true)
    public static Object[][] goodIntegers() {
        return new Object[][]{
            new Object[]{"0", 0},
            new Object[]{"1", 1},
            new Object[]{"-1", -1},
            new Object[]{"017", 17}, // Make sure it is interpreted as decimal, not octal.
            new Object[]{"2147483647", 2147483647},
            new Object[]{"-2147483648", -2147483648}};
    }

    @DataProvider(name = "badIntegers", parallel = true)
    public static Object[][] badIntegers() {
        return new Object[][]{
            new Object[]{""},
            new Object[]{"1-1"},
            new Object[]{"--1"},
            new Object[]{"null"},
            new Object[]{null},
            new Object[]{"zero"},
            new Object[]{"2147483648"},
            new Object[]{"-2147483649"}};
    }

    @DataProvider(name = "goodLongs", parallel = true)
    public static Object[][] goodLongs() {
        return new Object[][]{
            new Object[]{"0", 0L},
            new Object[]{"1", 1L},
            new Object[]{"-1", -1L},
            new Object[]{"017", 17L}, // Make sure it is interpreted as decimal, not octal.
            new Object[]{"2147483648", 2147483648L},
            new Object[]{"-2147483649", -2147483649L},
            new Object[]{"9223372036854775807", 9223372036854775807L},
            new Object[]{"-9223372036854775808", -9223372036854775808L},};
    }

    @DataProvider(name = "badLongs", parallel = true)
    public static Object[][] badLongs() {
        return new Object[][]{
            new Object[]{""},
            new Object[]{"1-1"},
            new Object[]{"--1"},
            new Object[]{"null"},
            new Object[]{null},
            new Object[]{"zero"},
            new Object[]{"9223372036854775808"},
            new Object[]{"-9223372036854775809"}};
    }

    @DataProvider(name = "goodBooleans", parallel = true)
    public static Object[][] goodBooleans() {
        return new Object[][]{
            new Object[]{"true", true},
            new Object[]{"false", false},
            new Object[]{"TRUE", true},
            new Object[]{"FALSE", false},
            new Object[]{"True", true},
            new Object[]{"False", false},
            new Object[]{"tRuE", true},
            new Object[]{"fALSe", false}};
    }

    @DataProvider(name = "badBooleans", parallel = true)
    public static Object[][] badBooleans() {
        return new Object[][]{
            new Object[]{""},
            new Object[]{"0"},
            new Object[]{"-1"},
            new Object[]{"1"},
            new Object[]{"null"},
            new Object[]{null},
            new Object[]{"trueish"},
            new Object[]{"falsey"},
            new Object[]{"yes"},
            new Object[]{"no"}};
    }

    @DataProvider(name = "goodStrings", parallel = true)
    public static Object[][] goodStrings() {
        return new Object[][]{
            new Object[]{""},
            new Object[]{"0"},
            new Object[]{"-1"},
            new Object[]{"1"},
            new Object[]{"null"},
            new Object[]{null},
            new Object[]{"trueish"},
            new Object[]{"falsey"},
            new Object[]{"yes"},
            new Object[]{"no"},
            new Object[]{"1-1"},
            new Object[]{"--1"},
            new Object[]{"null"},
            new Object[]{null},
            new Object[]{"zero"},
            new Object[]{"9223372036854775808"},
            new Object[]{"-9223372036854775809"},
            new Object[]{"Whatever string we give, we get back."}
        };
    }
}
