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
import com.redsaz.meterrier.api.model.Sample;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads PreSamples from a CSV-based JTL source.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvJtlSource implements Samples {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvJtlSource.class);

    private static final Set<JtlType> REQUIRED_COLUMNS = EnumSet.of(
            JtlType.TIMESTAMP, JtlType.ELAPSED, JtlType.LABEL,
            JtlType.RESPONSE_CODE, JtlType.THREAD_NAME, JtlType.SUCCESS,
            JtlType.BYTES, JtlType.ALL_THREADS);

    private final List<Sample> samples = new ArrayList<>();
    private long earliestMillis = Long.MAX_VALUE;
    private Sample earliest = null;
    private long latestMillis = Long.MIN_VALUE;
    private Sample latest = null;
    private final List<String> labels = new ArrayList<>();
    private final List<String> threadNames = new ArrayList<>();
    private final StatusCodeLookup statusCodeLookup = new StatusCodeLookup();

    public CsvJtlSource(File source) {
        try {
            long startMillis = System.currentTimeMillis();
            LOGGER.debug("Loading samples from file {}...", source);
            readCsvFile(source, labels, threadNames);
            LOGGER.debug("...took {}ms to read {} rows.",
                    System.currentTimeMillis() - startMillis,
                    samples.size());
        } catch (RuntimeException | IOException ex) {
            throw new AppServerException("Unable to convert file.", ex);
        }

    }

    @Override
    public List<Sample> getSamples() {
        return samples;
    }

    @Override
    public long getEarliestMillis() {
        return earliestMillis;
    }

    @Override
    public long getLatestMillis() {
        return latestMillis;
    }

    @Override
    public Sample getEarliestSample() {
        return earliest;
    }

    @Override
    public Sample getLatestSample() {
        return latest;
    }

    @Override
    public List<String> getLabels() {
        return Collections.unmodifiableList(labels);
    }

    @Override
    public List<String> getThreadNames() {
        return Collections.unmodifiableList(threadNames);
    }

    @Override
    public StatusCodeLookup getStatusCodeLookup() {
        return statusCodeLookup;
    }

    private void readCsvFile(File source, List<String> outLabels, List<String> outThreadNames) throws IOException {
        Set<String> readLabels = new HashSet<>();
        Set<String> readThreadNames = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(source))) {
            CsvParserSettings settings = new CsvParserSettings();
            CsvParser parser = new CsvParser(settings);
            parser.beginParsing(br);
            String[] row = parser.parseNext();
            if (row == null) {
                throw new RuntimeException("JTL (CSV) contained no data.");
            }
            JtlTypeColumns jtc = new JtlTypeColumns(row);
            if (jtc.headerAbsent()) {
                Sample psRow = jtc.convert(row);
                if (psRow != null) {
                    update(psRow);
                    readLabels.add(psRow.getLabel());
                    readThreadNames.add(psRow.getThreadName());
                }
            }
            while ((row = parser.parseNext()) != null) {
                Sample psRow = jtc.convert(row);
                if (psRow != null) {
                    update(psRow);
                    readLabels.add(psRow.getLabel());
                    readThreadNames.add(psRow.getThreadName());
                }
            }
            parser.stopParsing();
        }
        normalizeOffset();
        outLabels.addAll(readLabels);
        Collections.sort(outLabels);
        outThreadNames.addAll(readThreadNames);
        Collections.sort(outThreadNames);
    }

    private void update(Sample row) {
        samples.add(row);
        calcMinMax(row);
        statusCodeLookup.getRef(row.getStatusCode(), row.getStatusMessage());
        if (samples.size() % 1000000L == 0) {
            LOGGER.debug("\tRunning row total: {}", samples.size());
        }
    }

    private void calcMinMax(Sample row) {
        long timestamp = row.getOffset();
        if (timestamp < earliestMillis) {
            earliest = row;
            earliestMillis = timestamp;
        }
        if (timestamp > latestMillis) {
            latest = row;
            latestMillis = timestamp;
        }
        long duration = row.getDuration();
        long timeMillis = timestamp + duration;
        if (timeMillis > latestMillis) {
            latest = row;
            latestMillis = timeMillis;
        }
    }

    /**
     * At this point, all of the samples offsets are still in "timestamp" form. This will subtract
     * the earliest timestamp from all of the timestamps, so that they become 0-based offsets, 0
     * being when the test began, rather than the UNIX epoch.
     */
    private void normalizeOffset() {
        for (Sample sample : samples) {
            sample.setOffset(sample.getOffset() - earliestMillis);
        }
    }

    private static class JtlTypeColumns {

        private List<JtlType> colTypes;
        private final boolean headerAbsent;
        // Rather than have potentially a bunch of instances of identical strings, store previously
        // seen strings (rather than use String.intern()
        private final Map<String, String> stringPool = new HashMap<>();

        public JtlTypeColumns(String[] header) {
            if (HeaderCheckUtil.isJtlHeaderRow(header)) {
                headerAbsent = false;
                colTypes = new ArrayList<>(header.length);
                for (String headerCol : header) {
                    JtlType type = JtlType.fromHeader(headerCol);
                    if (type == null) {
                        LOGGER.warn("Ignoring unknown header column \"{}\".", headerCol);
                    }
                    colTypes.add(type);
                }
            } else {
                headerAbsent = true;
                if (HeaderCheckUtil.canUseDefaultHeaderRow(header)) {
                    LOGGER.warn("The JTL (CSV) file seems to be missing the header row. Using the expected defaults.");
                    colTypes = HeaderCheckUtil.DEFAULT_HEADERS;
                } else {
                    LOGGER.error("No header row defined for JTL (CSV), and columns do not appear to be the defaults. Cannot convert.");
                    throw new IllegalArgumentException("Cannot convert from a JTL (CSV) with no header row and non-default column.");
                }
            }

            if (!colTypes.containsAll(REQUIRED_COLUMNS)) {
                throw new IllegalArgumentException("Cannot convert from a JTL (CSV) when not all of the following columns are included: " + REQUIRED_COLUMNS);
            }
        }

        public boolean headerAbsent() {
            return headerAbsent;
        }

        public boolean canConvert() {
            return true;
        }

        /**
         * Converts the row into a typed row. If the number of columns don't match the expected
         * columns, null is returned.
         *
         * @param row what to convert
         * @return a typed row, or null if it couldn't be converted.
         */
        public Sample convert(String[] row) {
            if (row.length != colTypes.size()) {
                LOGGER.warn("Skipping bad row. Expected {} columns but got {}. Contents:\n{}",
                        colTypes.size(), row.length, Arrays.toString(row));
                return null;
            }
            try {
                Sample out = new Sample();
                for (int i = 0; i < row.length; ++i) {
                    String colVal = row[i];
                    JtlType colType = colTypes.get(i);
                    if (colType != null) {
                        colType.putIn(out, colVal);
                    }
                }
                out.setLabel(stringPoolerize(out.getLabel()));
                out.setStatusCode(stringPoolerize(out.getStatusCode()));
                out.setStatusMessage(stringPoolerize(out.getStatusMessage()));
                out.setThreadName(stringPoolerize(out.getThreadName()));
                return out;
            } catch (NumberFormatException ex) {
                LOGGER.warn("Skipping bad row. Encountered {} when converting row. Contents:\n{}",
                        ex.getMessage(), Arrays.toString(row));
                return null;
            }
        }

        // Like String.intern(), but not global
        private String stringPoolerize(String value) {
            if (value == null) {
                return null;
            }
            String old = stringPool.get(value);
            if (old == null) {
                stringPool.put(value, value);
                old = value;
            }
            return old;
        }
    }
}
