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

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.redsaz.meterrier.api.exceptions.AppServerException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert a CSV-based JTL file into an Avro file.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvJtlToCsvJtlConverter implements Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvJtlToCsvJtlConverter.class);

    @Override
    public void convert(File source, File dest) {
        long startMillis = System.currentTimeMillis();
        long totalRows = 0;
        LOGGER.debug("Converting {} to {}...", source, dest);
        try {
            BufferedReader br = new BufferedReader(new FileReader(source));
            CSVReader reader = new CSVReader(br);
            Iterator<String[]> csvIter = reader.iterator();
            JtlRowToJtlRow j2j = null;
            if (csvIter.hasNext()) {
                String[] headers = csvIter.next();
                j2j = new JtlRowToJtlRow(headers,
                        JtlType.TIMESTAMP,
                        JtlType.ELAPSED,
                        JtlType.LABEL,
                        JtlType.RESPONSE_CODE,
                        JtlType.RESPONSE_MESSAGE,
                        JtlType.THREAD_NAME,
                        JtlType.SUCCESS,
                        JtlType.BYTES,
                        JtlType.SENT_BYTES,
                        JtlType.ALL_THREADS,
                        JtlType.URL
                );
            } else {
                throw new RuntimeException("No headers defined.");
            }

            try (
                    BufferedWriter bw = new BufferedWriter(new FileWriter("jtls/real-columntrimmed.jtl"));
                    CSVWriter writer = new CSVWriter(bw)) {
                writer.writeNext(j2j.getHeaders(), false);
                while (csvIter.hasNext()) {
                    String[] row = csvIter.next();
                    ++totalRows;
                    writer.writeNext(j2j.convert(row), false);
                }
            }
        } catch (RuntimeException | IOException ex) {
            throw new AppServerException("Unable to process import.", ex);
        }
        LOGGER.debug("{}ms to convert {} rows.", (System.currentTimeMillis() - startMillis), totalRows);
    }

}
