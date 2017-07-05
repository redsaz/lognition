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

import com.redsaz.meterrier.api.exceptions.AppServerException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert a CSV-based JTL file into an Avro file.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvJtlToAvroOrderedConverter implements Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvJtlToAvroOrderedConverter.class);
    private static final SamplesWriter writer = new AvroSamplesWriter();

    public static void main(String[] args) throws IOException {
//        File source = new File("../meterrier/jtls/target/real-large.jtl");
//        File dest = new File("../meterrier/jtls/target/converted/real-large.avro");

//        File source = new File("../meterrier/jtls/target/real-without-header.jtl");
//        File dest = new File("../meterrier/jtls/target/converted/real-without-header.avro");
        File source = new File("../meterrier/jtls/target/real-550cps-1hour.jtl");
        File dest = new File("../meterrier/jtls/target/converted/real-550cps-1hour.avro");
        Converter conv = new CsvJtlToAvroOrderedConverter();
        conv.convert(source, dest);
    }

    @Override
    public String convert(File source, File dest) {
        try {
            long startMillis = System.currentTimeMillis();
            LOGGER.debug("Converting {} to {}...", source, dest);
            Samples sourceSamples = new CsvJtlSource(source);
            Collections.sort(sourceSamples.getSamples());
            long totalRows = sourceSamples.getSamples().size();
            LOGGER.debug("...took {}ms to read and sort {} rows. Creating dest={}...",
                    System.currentTimeMillis() - startMillis,
                    totalRows,
                    dest);
            String sha256Hash = writer.write(sourceSamples, dest);
            LOGGER.debug("{}ms to convert {} rows to {}.",
                    (System.currentTimeMillis() - startMillis), totalRows, dest);
            return sha256Hash;
        } catch (RuntimeException | IOException ex) {
            throw new AppServerException("Unable to convert file.", ex);
        }
    }

}
