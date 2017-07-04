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

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert a CSV-based JTL file into another CSV-based JTL file, but with different columns.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvJtlToCsvJtlConverter implements Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvJtlToCsvJtlConverter.class);

    @Override
    public String convert(File source, File dest) {
        long startMillis = System.currentTimeMillis();
        long totalRows = 0;
        LOGGER.debug("Converting {} to {}...", source, dest);
        String sha256Hash = null;
        try (BufferedReader br = new BufferedReader(new FileReader(source))) {
            JtlRowToJtlRow j2j = null;
            CsvParserSettings settings = new CsvParserSettings();
            CsvParser parser = new CsvParser(settings);
            parser.beginParsing(br);
            String[] firstLine = parser.parseNext();
            if (firstLine == null) {
                throw new RuntimeException("JTL (CSV) contained no data.");
            }
            boolean firstLineIsHeader;
            if (HeaderCheckUtil.isJtlHeaderRow(firstLine)) {
                j2j = new JtlRowToJtlRow(firstLine,
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
                firstLineIsHeader = true;
            } else if (HeaderCheckUtil.canUseDefaultHeaderRow(firstLine)) {
                LOGGER.warn("The JTL (CSV) file seems to be missing the header row. Using the expected defaults.");
                j2j = new JtlRowToJtlRow(HeaderCheckUtil.DEFAULT_HEADERS_TEXT,
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
                firstLineIsHeader = false;
            } else {
                throw new IllegalArgumentException("Cannot convert from a JTL (CSV) with no header row and non-default column.");
            }

            try (HashingOutputStream hos = new HashingOutputStream(Hashing.sha256(), new BufferedOutputStream(new FileOutputStream(dest)))) {
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(hos, "UTF-8"))) {
                    CsvWriter writer = null;
                    try {
                        writer = new CsvWriter(bw, new CsvWriterSettings());

                        writer.writeHeaders(j2j.getHeaders());
                        if (!firstLineIsHeader) {
                            ++totalRows;
                            writer.writeRow(j2j.convert(firstLine));
                        }
                        String[] row;
                        while ((row = parser.parseNext()) != null) {
                            ++totalRows;
                            writer.writeRow(j2j.convert(row));
                        }
                    } finally {
                        if (writer != null) {
                            writer.close();
                        }
                    }
                }
                sha256Hash = hos.hash().toString();
            }
        } catch (RuntimeException | IOException ex) {
            throw new AppServerException("Unable to convert file.", ex);
        }
        LOGGER.debug("{}ms to convert {} rows.", (System.currentTimeMillis() - startMillis), totalRows);
        return sha256Hash;
    }

}
