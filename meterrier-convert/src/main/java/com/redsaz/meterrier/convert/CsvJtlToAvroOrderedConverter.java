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
import com.redsaz.meterrier.convert.model.HttpSample;
import com.redsaz.meterrier.convert.model.PreSample;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert a CSV-based JTL file into an Avro file.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvJtlToAvroOrderedConverter implements Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvJtlToAvroOrderedConverter.class);

    private static final Set<JtlType> REQUIRED_COLUMNS = EnumSet.of(
            JtlType.TIMESTAMP, JtlType.ELAPSED, JtlType.LABEL,
            JtlType.RESPONSE_CODE, JtlType.THREAD_NAME, JtlType.SUCCESS,
            JtlType.BYTES, JtlType.ALL_THREADS);

    @Override
    public String convert(File source, File dest) {
        long startMillis = System.currentTimeMillis();
        long totalRows = 0;
        String sha256Hash = null;
        try {
            LOGGER.debug("Converting {} to {}...", source, dest);
            IntermediateInfo info = csvToIntermediate(source);
            LOGGER.debug("...took {}ms to read and sort {} rows. Creating dest={}...",
                    System.currentTimeMillis() - startMillis,
                    info.numRows,
                    dest);
            sha256Hash = info.writeAvro(dest);
            totalRows = info.numRows;
            LOGGER.debug("{}ms to convert {} rows to {}.",
                    (System.currentTimeMillis() - startMillis), totalRows, dest);
        } catch (RuntimeException | IOException ex) {
            throw new AppServerException("Unable to convert file.", ex);
        }
        return sha256Hash;
    }

    private IntermediateInfo csvToIntermediate(File source) throws IOException {
        IntermediateInfo info = new IntermediateInfo();
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
                PreSample psRow = jtc.convert(row);
                if (psRow != null) {
                    info.update(psRow);
                }
            }
            while ((row = parser.parseNext()) != null) {
                PreSample psRow = jtc.convert(row);
                if (psRow != null) {
                    info.update(psRow);
                }
            }
            parser.stopParsing();
        }
        info.sort();
        return info;
    }

    private static HttpSample createNewEmptyHttpSample() {
        HttpSample hs = new HttpSample();
        hs.setMillisElapsed(-1L);
        hs.setResponseBytes(-1L);
        return hs;
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
        public PreSample convert(String[] row) {
            if (row.length != colTypes.size()) {
                LOGGER.warn("Skipping bad row. Expected {} columns but got {}. Contents:\n{}",
                        colTypes.size(), row.length, Arrays.toString(row));
                return null;
            }
            try {
                PreSample out = new PreSample();
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

    private static class IntermediateInfo {

        // Because we don't care about syncing, but DO care about repeatably
        // creating the same output data given the same input data, we'll use
        // our own sync marker rather than the randomly generated one that avro
        // gives us.
        private static final byte[] SYNC = new byte[16];

        long earliest = Long.MAX_VALUE;
        long latest = Long.MIN_VALUE;
        long numRows;
        // "Pre-references" are references for what the real references will be.
        // Since labels, urls, and threadnames will be sorted by name, and since we
        // won't know all the values until the end, we need to have temporary references
        // which will be replaced by sorted references later.
        private final Set<String> prelabel = new HashSet<>();
        private final Set<String> prethreadNames = new HashSet<>();
        private final StatusCodeLookup statusCodeLookup = new StatusCodeLookup();
        private final List<PreSample> presamples = new ArrayList<>(1000000);

        public void update(PreSample row) {
            ++numRows;
            Runtime.getRuntime().freeMemory();
            calcTimes(row.getOffset(), row.getDuration());
            if (row.getLabel() != null) {
                prelabel.add(row.getLabel());
            }
            if (row.getThreadName() != null) {
                prethreadNames.add(row.getThreadName());
            }
            if (row.getStatusCode() != null) {
                statusCodeLookup.getRef(row.getStatusCode(), row.getStatusMessage());
            }
            presamples.add(row);
            if (numRows % 1000000L == 0) {
                LOGGER.debug("Read {} rows for intermediate file so far. {} MB Free, {} Max MB", numRows, Runtime.getRuntime().freeMemory() / 1024 / 1024, Runtime.getRuntime().maxMemory() / 1024 / 1024);
            }
        }

        public void sort() {
            Collections.sort(presamples);
        }

        /**
         * Converts the intermediate information into the final format, sorting the entries
         * chronologically, then by length of time to complete the operation, then by thread name.
         *
         * @param dest Destination of the final format
         * @throws IOException If the input could not be read or the output could not be written.
         */
        public String writeAvro(File dest) throws IOException {
            String sha256Hash = null;
            if (dest.exists()) {
                LOGGER.debug("File \"{}\" already exists. It will be replaced.", dest);
            }
            DatumWriter<HttpSample> httpSampleDatumWriter = new SpecificDatumWriter<>(HttpSample.class);
            List<CharSequence> labels = createSortedList(prelabel);
            Map<CharSequence, Integer> labelLookup = createLookup(labels);
            List<CharSequence> threadNames = createSortedList(prethreadNames);
            Map<CharSequence, Integer> threadNameLookup = createLookup(threadNames);
            try (HashingOutputStream hos = new HashingOutputStream(Hashing.sha256(), new BufferedOutputStream(new FileOutputStream(dest)))) {
                try (DataFileWriter<HttpSample> dataFileWriter = new DataFileWriter<>(httpSampleDatumWriter)) {
                    dataFileWriter.setMeta("earliest", earliest);
                    dataFileWriter.setMeta("latest", latest);
                    dataFileWriter.setMeta("numRows", numRows);

                    if (!labels.isEmpty()) {
                        writeMetaStringArray(dataFileWriter, "labels", labels);
                    }

                    if (!threadNames.isEmpty()) {
                        writeMetaStringArray(dataFileWriter, "threadNames", threadNames);
                    }

                    List<CharSequence> codes = statusCodeLookup.getCustomCodes();
                    List<CharSequence> messages = statusCodeLookup.getCustomMessages();
                    if (codes != null && !codes.isEmpty()) {
                        writeMetaStringArray(dataFileWriter, "codes", codes);
                        writeMetaStringArray(dataFileWriter, "messages", messages);
                    }
                    dataFileWriter.create(HttpSample.getClassSchema(), hos, SYNC);

                    long numRowsWritten = 0;
                    long writeStartMs = System.currentTimeMillis();
                    for (PreSample presample : presamples) {
                        HttpSample httpSample = convert(presample, labelLookup, threadNameLookup);
                        dataFileWriter.append(httpSample);
                        ++numRowsWritten;
                        if (numRowsWritten % 1000000L == 0) {
                            LOGGER.debug("{}ms to write {} of {} rows so far.",
                                    System.currentTimeMillis() - writeStartMs,
                                    numRowsWritten, numRows);
                        }
                    }
                }
                sha256Hash = hos.hash().toString();
            }
            return sha256Hash;
        }

        private static List<CharSequence> createSortedList(Collection<String> items) {
            SortedSet<String> sortedSet = new TreeSet<>(items);
            List<CharSequence> list = new ArrayList<>(sortedSet.size());
            sortedSet.stream().forEach((item) -> {
                list.add(new Utf8(item));
            });
            return list;
        }

        // This iterable better not change between when this is called and when
        // the array is made, otherwise it'll be all sorts of messed up and you
        // won't be able to know.
        private static Map<CharSequence, Integer> createLookup(Iterable<CharSequence> items) {
            Map<CharSequence, Integer> lookup = new HashMap<>();
            Integer ref = 1;
            for (CharSequence item : items) {
                lookup.put(item.toString(), ref);
                ++ref;
            }
            return lookup;
        }

        private static void writeMetaStringArray(DataFileWriter<?> dataFileWriter, String name, Collection<CharSequence> items) throws IOException {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Encoder enc = EncoderFactory.get().directBinaryEncoder(baos, null);
                enc.writeArrayStart();
                enc.setItemCount(items.size());
                for (CharSequence item : items) {
                    enc.writeString(item);
                }
                enc.writeArrayEnd();
                dataFileWriter.setMeta(name, baos.toByteArray());
            }
        }

        private HttpSample convert(PreSample row,
                Map<CharSequence, Integer> labelLookup,
                Map<CharSequence, Integer> threadNameLookup) {
            HttpSample hs = createNewEmptyHttpSample();
            hs.setResponseBytes(longOrDefault(row.getResponseBytes(), -1));
            hs.setTotalThreads(intOrDefault(row.getTotalThreads(), 0));
            int labelRef = labelLookup.getOrDefault(row.getLabel(), 0);
            if (labelRef < 1) {
                LOGGER.warn("Bad labelRef={}", labelRef);
            }
            hs.setLabelRef(labelRef);
            hs.setMillisElapsed(longOrDefault(row.getDuration(), 0));
            hs.setMillisOffset(row.getOffset() - earliest);
            hs.setResponseCodeRef(statusCodeLookup.getRef(row.getStatusCode(), row.getStatusMessage()));
            hs.setSuccess(booleanOrDefault(row.isSuccess(), true));
            hs.setThreadNameRef(threadNameLookup.getOrDefault(row.getThreadName(), 0));

            return hs;
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
}
