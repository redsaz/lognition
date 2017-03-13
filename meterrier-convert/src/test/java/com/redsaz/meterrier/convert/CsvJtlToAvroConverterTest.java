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
import java.io.IOException;
import java.util.Arrays;
import org.testng.annotations.Test;

/**
 * Test The CSV-JTL to Avro converter.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvJtlToAvroConverterTest extends ConverterBaseTest {

    @Test
    public void testConvert() throws IOException {
        // The orginal CSV to import from will have more columns than we'll
        // actually use.
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
        File source = createTempFile("source", ".csv");
        mpd.createImportCsvFile(source);

        File expectedDest = createTempFile("expected", ".avro");
        mpd.createAvroFile(expectedDest);

        Converter conv = new CsvJtlToAvroConverter();
        File actualDest = createTempFile("actual", ".avro");
        conv.convert(source, actualDest);

        assertAvroContentEquals(actualDest, expectedDest, "The converter did not convert in the way expected.");
    }

}
