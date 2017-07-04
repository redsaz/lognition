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

import java.io.File;
import java.util.Arrays;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 * Test the Avro to CSV-JTL converter.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class AvroToCsvJtlConverterTest extends ConverterBaseTest {

    @Test
    public void testConvert() {
        MockPerfData mpd = new MockPerfData(System.currentTimeMillis(),
                240L,
                Arrays.asList(
                        "Another-call-2",
                        "Howdy there this is a call as well",
                        "example-call-1"
                ),
                Arrays.asList(
                        "thread-1",
                        "thread-2",
                        "thread-3",
                        "thread-4",
                        "thread-5"
                ),
                Arrays.asList(
                        "1001",
                        "200"
                ),
                Arrays.asList(
                        "Non Standard code",
                        "Normally we don't see these"
                ));
        File source = createTempFile("source", ".avro");
        mpd.createAvroFile(source);

        File expectedDest = createTempFile("expected", ".csv");
        String expectedHash = mpd.createExportedCsvFile(expectedDest);

        Converter conv = new AvroToCsvJtlConverter();
        File actualDest = createTempFile("actual", ".csv");
        String actualHash = conv.convert(source, actualDest);

        assertContentEquals(actualDest, expectedDest, "The converter did not convert in the way expected.");
        assertBytesEquals(actualDest, expectedDest, "The conversions are not byte-for-byte equal.");
        assertEquals(actualHash, expectedHash, "Hashes differed.");
    }

}
