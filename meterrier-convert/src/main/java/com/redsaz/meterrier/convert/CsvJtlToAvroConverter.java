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

import com.opencsv.CSVReader;
import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.redsaz.meterrier.convert.model.Entry;
import com.redsaz.meterrier.convert.model.HttpSample;
import com.redsaz.meterrier.convert.model.Metadata;
import com.redsaz.meterrier.convert.model.StringArray;
import com.redsaz.meterrier.convert.model.jmeter.CsvJtlRow;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert a CSV-based JTL file into an Avro file.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvJtlToAvroConverter implements Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvJtlToAvroConverter.class);
    // Sometimes (in JMeter versions 2.12 and possibly earlier), when running
    // JMeter in remote mode, the CSV JTL file will not have a header row! The
    // CSV JTL generated in this fashion usually has the following defaults:
    // timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency
    // So, we will attempt to detect for this case and compensate appropriately.
    private static final List<JtlType> DEFAULT_HEADERS = Arrays.asList(
            JtlType.TIMESTAMP, JtlType.ELAPSED, JtlType.LABEL,
            JtlType.RESPONSE_CODE, JtlType.RESPONSE_MESSAGE, JtlType.THREAD_NAME,
            JtlType.DATA_TYPE, JtlType.SUCCESS, JtlType.BYTES, JtlType.GRP_THREADS,
            JtlType.ALL_THREADS, JtlType.LATENCY);

    private static final Set<JtlType> REQUIRED_COLUMNS = EnumSet.of(
            JtlType.TIMESTAMP, JtlType.ELAPSED, JtlType.LABEL,
            JtlType.RESPONSE_CODE, JtlType.THREAD_NAME, JtlType.SUCCESS,
            JtlType.BYTES, JtlType.ALL_THREADS);

    @Override
    public void convert(File source, File dest) {
        File intermediateFile = new File(dest.getParent(), dest.getName() + ".intermediate");
        long startMillis = System.currentTimeMillis();
        long totalRows = 0;
        try {
            LOGGER.debug("Converting {} to {}...", source, dest);
            IntermediateInfo info = csvToIntermediate(source, intermediateFile);
            info.writeAvro(intermediateFile, dest);
            totalRows = info.numRows;
        } catch (RuntimeException | IOException ex) {
            throw new AppServerException("Unable to convert file.", ex);
        } finally {
            if (intermediateFile.exists()) {
                boolean success = intermediateFile.delete();
                if (!success) {
                    LOGGER.error("Could not delete intermediate file {}! It is no longer needed and must be deleted manually.", intermediateFile);
                }
            }
        }
        LOGGER.debug("{}ms to convert {} rows to {}.",
                (System.currentTimeMillis() - startMillis), totalRows, dest);
    }

    private IntermediateInfo csvToIntermediate(File source, File dest) throws IOException {
        DatumWriter<CsvJtlRow> userDatumWriter = new SpecificDatumWriter<>(CsvJtlRow.class);
        IntermediateInfo info = new IntermediateInfo();
        try (BufferedReader br = new BufferedReader(new FileReader(source));
                CSVReader reader = new CSVReader(br);
                DataFileWriter<CsvJtlRow> dataFileWriter = new DataFileWriter<>(userDatumWriter)) {
            Iterator<String[]> csvIter = reader.iterator();
            if (!csvIter.hasNext()) {
                throw new RuntimeException("JTL (CSV) contained no data.");
            }
            dataFileWriter.create(CsvJtlRow.getClassSchema(), dest);
            String[] row = csvIter.next();
            JtlTypeColumns jtc = new JtlTypeColumns(row);
            if (jtc.headerAbsent()) {
                CsvJtlRow cjRow = jtc.convert(row);
                if (cjRow != null) {
                    info.update(cjRow);
                    dataFileWriter.append(cjRow);
                }
            }
            while (csvIter.hasNext()) {
                row = csvIter.next();
                CsvJtlRow cjRow = jtc.convert(row);
                if (cjRow != null) {
                    info.update(cjRow);
                    dataFileWriter.append(cjRow);
                }
            }
        }
        return info;
    }

    private static class JtlTypeColumns {

        private List<JtlType> colTypes;
        private final boolean headerAbsent;

        public JtlTypeColumns(String[] header) {
            colTypes = new ArrayList<>(header.length);
            int numUnknownCols = 0;
            for (String headerCol : header) {
                JtlType type = JtlType.fromHeader(headerCol);
                if (type == null) {
                    LOGGER.warn("Ignoring unknown header column \"{}\".", headerCol);
                    ++numUnknownCols;
                }
                colTypes.add(type);
            }
            // If we don't have enough known header columns, and the number of
            // columns is exactly 12, then we may have hit a bug with Jmeter 2.12
            // in which the JTL CSV headers are not output by default in remote
            // mode. Thankfully it can be fixable.
            headerAbsent = header.length > 3 && header.length - numUnknownCols < 3;
            if (headerAbsent) {
                if (header.length == 12
                        && isNumber(header[0]) && isNumber(header[1])
                        && isBoolean(header[7])
                        && isNumber(header[8]) && isNumber(header[9])
                        && isNumber(header[10]) && isNumber(header[11])) {
                    LOGGER.warn("The JTL (CSV) file seems to be missing the header row. Using the expected defaults.");
                    colTypes = DEFAULT_HEADERS;
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
         * Converts the row into a typed row. If the number of columns don't
         * match the expected columns, null is returned.
         *
         * @param row what to convert
         * @return a typed row, or null if it couldn't be converted.
         */
        public CsvJtlRow convert(String[] row) {
            if (row.length != colTypes.size()) {
                LOGGER.warn("Skipping bad row. Expected {} columns but got {}. Contents:\n{}",
                        colTypes.size(), row.length, Arrays.toString(row));
                return null;
            }
            try {
                CsvJtlRow out = new CsvJtlRow();
                for (int i = 0; i < row.length; ++i) {
                    String colVal = row[i];
                    JtlType colType = colTypes.get(i);
                    if (colType != null) {
                        colType.putIn(out, colVal);
                    }
                }
                return out;
            } catch (NumberFormatException ex) {
                LOGGER.warn("Skipping bad row. Encountered {} when converting row. Contents:\n{}",
                        ex.getMessage(), Arrays.toString(row));
                return null;
            }
        }

        private static boolean isNumber(String text) {
            if (text == null || text.isEmpty()) {
                return false;
            }
            char first = text.charAt(0);
            if (first != '-' && first != '+' && (first < '0' || first > '9')) {
                return false;
            }
            for (int i = 1; i < text.length(); ++i) {
                char c = text.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
            }
            return true;
        }

        private static boolean isBoolean(String text) {
            if (text == null || text.isEmpty()) {
                return false;
            }
            return "true".equalsIgnoreCase("true") || "false".equalsIgnoreCase("false");
        }

    }

    private static class IntermediateInfo {

        long earliest = Long.MAX_VALUE;
        long latest = Long.MIN_VALUE;
        long numRows;
        private final SortedSet<CharSequence> labels = new TreeSet<>();
        private final SortedSet<CharSequence> urls = new TreeSet<>();
        private final SortedSet<CharSequence> threadNames = new TreeSet<>();
        private final StatusCodeLookup statusCodeLookup = new StatusCodeLookup();

        public IntermediateInfo() {
        }

        public void update(CsvJtlRow row) {
            ++numRows;
            calcTimes(row.getTimeStamp(), row.getElapsed());
            if (row.getLabel() != null) {
                labels.add(row.getLabel());
            }
            if (row.getURL() != null) {
                urls.add(row.getURL());
            }
            if (row.getThreadName() != null) {
                threadNames.add(row.getThreadName());
            }
            if (row.getResponseCode() != null) {
                statusCodeLookup.getRef(row.getResponseCode(), row.getResponseMessage());
            }
        }

        /**
         * Converts the intermediate information into the final format.
         *
         * @param intermediateSource Has the intermediate data
         * @param dest Destination of the final format
         * @throws IOException If the input could not be read or the output
         * could not be written.
         */
        public void writeAvro(File intermediateSource, File dest) throws IOException {
            DatumWriter<Entry> userDatumWriter = new SpecificDatumWriter<>(Entry.class);
            DatumReader<CsvJtlRow> userDatumReader = new SpecificDatumReader<>(CsvJtlRow.class);
            try (DataFileWriter<Entry> dataFileWriter = new DataFileWriter<>(userDatumWriter);
                    DataFileReader<CsvJtlRow> reader = new DataFileReader<>(intermediateSource, userDatumReader)) {
                dataFileWriter.create(Entry.getClassSchema(), dest);

                dataFileWriter.append(new Entry(new Metadata(earliest, latest, numRows)));
                Map<CharSequence, Integer> labelLookup = createLookup(labels);
                if (!labels.isEmpty()) {
                    Entry labelEntry = new Entry(new StringArray("labels", new ArrayList<>(labels)));
                    dataFileWriter.append(labelEntry);
                }
                Map<CharSequence, Integer> threadNameLookup = createLookup(threadNames);
                if (!threadNames.isEmpty()) {
                    Entry threadNamesEntry = new Entry(new StringArray("threadNames", new ArrayList<>(threadNames)));
                    dataFileWriter.append(threadNamesEntry);
                }
                Map<CharSequence, Integer> urlLookup = createLookup(urls);
                if (!urls.isEmpty()) {
                    Entry urlsEntry = new Entry(new StringArray("urls", new ArrayList<>(urls)));
                    dataFileWriter.append(urlsEntry);
                }
                List<CharSequence> codes = statusCodeLookup.getCustomCodes();
                List<CharSequence> messages = statusCodeLookup.getCustomMessages();
                if (codes != null && !codes.isEmpty()) {
                    Entry codesEntry = new Entry(new StringArray("codes", codes));
                    dataFileWriter.append(codesEntry);
                    Entry messagesEntry = new Entry(new StringArray("messages", messages));
                    dataFileWriter.append(messagesEntry);
                }
                while (reader.hasNext()) {
                    CsvJtlRow row = reader.next();
                    Entry entry = convert(row, labelLookup, threadNameLookup, urlLookup);
                    dataFileWriter.append(entry);
                }
            }
        }

        // This iterable better not change between when this is called and when
        // the array is made, otherwise it'll be all sorts of messed up and you
        // won't be able to know.
        private Map<CharSequence, Integer> createLookup(Iterable<CharSequence> items) {
            Map<CharSequence, Integer> lookup = new HashMap<>();
            Integer ref = 1;
            for (CharSequence item : items) {
                lookup.put(new Utf8(item.toString()), ref);
                ++ref;
            }
            return lookup;
        }

        private Entry convert(CsvJtlRow row,
                Map<CharSequence, Integer> labelLookup,
                Map<CharSequence, Integer> threadNameLookup,
                Map<CharSequence, Integer> urlLookup) {
            HttpSample hs = createNewEmptyHttpSample();
            hs.setResponseBytes(longOrDefault(row.getBytes(), -1));
            hs.setTotalThreads(intOrDefault(row.getAllThreads(), 0));
            int labelRef = labelLookup.getOrDefault(row.getLabel(), 0);
            if (labelRef < 1) {
                LOGGER.warn("Bad labelRef={}", labelRef);
            }
            hs.setLabelRef(labelRef);
            hs.setMillisElapsed(longOrDefault(row.getElapsed(), 0));
            hs.setMillisOffset(row.getTimeStamp() - earliest);
            hs.setResponseCodeRef(statusCodeLookup.getRef(row.getResponseCode(), row.getResponseMessage()));
            hs.setSuccess(booleanOrDefault(row.getSuccess(), true));
            hs.setThreadNameRef(threadNameLookup.getOrDefault(row.getThreadName(), 0));

            return new Entry(hs);
        }

        private long longOrDefault(Long value, long defaultVal) {
            if (value == null) {
                return defaultVal;
            }
            return value;
        }

        private int intOrDefault(Integer value, int defaultVal) {
            if (value == null) {
                return defaultVal;
            }
            return value;
        }

        private boolean booleanOrDefault(Boolean value, boolean defaultVal) {
            if (value == null) {
                return defaultVal;
            }
            return value;
        }

        private void calcTimes(Long timestamp, Long elapsed) {
            if (timestamp == null) {
                return;
            }
            if (timestamp < earliest) {
                earliest = timestamp;
            }
            if (timestamp > latest) {
                latest = timestamp;
            }
            if (elapsed != null) {
                long timeMillis = timestamp + elapsed;
                if (timeMillis > latest) {
                    latest = timeMillis;
                }
            }
        }

    }

    private static HttpSample createNewEmptyHttpSample() {
        HttpSample hs = new HttpSample();
        hs.setMillisElapsed(-1L);
        hs.setResponseBytes(-1L);
        return hs;
    }
}