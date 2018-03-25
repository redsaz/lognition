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
import org.testng.annotations.Test;

/**
 * Tests for the JtlRowToJtlRow class.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class JtlRowToJtlRowTest {

    @Test
    public void testJtlRowToJtlRowTest() {
        String[] headers = new String[]{"timeStamp", "elapsed", "label",
            "responseCode", "responseMessage", "threadName", "dataType",
            "success", "bytes", "grpThreads", "allThreads", "Latency"};
        JtlRowToJtlRow jr2jr = new JtlRowToJtlRow(headers,
                JtlType.TIMESTAMP, JtlType.ELAPSED, JtlType.LABEL,
                JtlType.SUCCESS, JtlType.VARIABLES);
        assertEquals(jr2jr.getHeaders(),
                new String[]{"timeStamp", "elapsed", "label", "success"},
                "Should only include headers that are in BOTH the original headers and in the desired headers.");
    }

    @Test
    public void testGetHeaders() {
        String[] headers = new String[]{"timeStamp", "elapsed", "label",
            "responseCode", "responseMessage", "threadName", "dataType",
            "success", "bytes", "grpThreads", "allThreads", "Latency"};
        JtlRowToJtlRow jr2jr = new JtlRowToJtlRow(headers,
                JtlType.TIMESTAMP, JtlType.ELAPSED, JtlType.LABEL,
                JtlType.SUCCESS, JtlType.VARIABLES);
        assertEquals(jr2jr.getHeaders(),
                new String[]{"timeStamp", "elapsed", "label", "success"},
                "Should only include headers that are in BOTH the original headers and in the desired headers.");
    }

    @Test
    public void testConvert() {
        String[] headers = new String[]{"timeStamp", "elapsed", "label",
            "responseCode", "responseMessage", "threadName", "dataType",
            "success", "bytes", "grpThreads", "allThreads", "Latency"};
        JtlRowToJtlRow jr2jr = new JtlRowToJtlRow(headers,
                JtlType.TIMESTAMP, JtlType.ELAPSED, JtlType.LABEL,
                JtlType.SUCCESS, JtlType.VARIABLES);
        String[] actual = jr2jr.convert(new String[]{"1234", "10", "GET site",
            "200", "Success", "Thread-1", "text",
            "true", "123", "4", "5", "1"});
        String[] expected = new String[]{"1234", "10", "GET site",
            "true"};
        assertEquals(actual, expected, "Incorrect row values retained.");
    }

}
