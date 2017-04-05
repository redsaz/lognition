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
import com.opencsv.CSVReader;
import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.redsaz.meterrier.convert.model.HttpSample;
import com.redsaz.meterrier.convert.model.jmeter.CsvJtlRow;
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
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
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

    private static final Set<JtlType> REQUIRED_COLUMNS = EnumSet.of(
            JtlType.TIMESTAMP, JtlType.ELAPSED, JtlType.LABEL,
            JtlType.RESPONSE_CODE, JtlType.THREAD_NAME, JtlType.SUCCESS,
            JtlType.BYTES, JtlType.ALL_THREADS);

    @Override
    public String convert(File source, File dest) {
        File intermediateFile = new File(dest.getParent(), dest.getName() + ".intermediate");
        long startMillis = System.currentTimeMillis();
        long totalRows = 0;
        String sha256Hash = null;
        try {
            LOGGER.debug("Converting {} to {}...", source, dest);
            LOGGER.debug("Creating intermediate file...");
            IntermediateInfo info = csvToIntermediate(source, intermediateFile);
            LOGGER.debug("...intermediate file created in {}ms after reading {} rows. Creating dest={}...",
                    System.currentTimeMillis() - startMillis,
                    info.numRows,
                    dest);
            sha256Hash = info.writeAvro(intermediateFile, dest);
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
        return sha256Hash;
    }

    private IntermediateInfo csvToIntermediate(File source, File dest) throws IOException {
        DatumWriter<CsvJtlRow> userDatumWriter = new SpecificDatumWriter<>(CsvJtlRow.class);
        IntermediateInfo info = new IntermediateInfo();
        if (dest.exists()) {
            LOGGER.debug("File \"{}\" already exists. It will be replaced.", dest);
        }
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
        private final SortedSet<CharSequence> labels = new TreeSet<>();
        private final SortedSet<CharSequence> urls = new TreeSet<>();
        private final SortedSet<CharSequence> threadNames = new TreeSet<>();
        private final StatusCodeLookup statusCodeLookup = new StatusCodeLookup();

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
            if (numRows % 1000000L == 0) {
                LOGGER.debug("Read {} rows for intermediate file so far.", numRows);
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
        public String writeAvro(File intermediateSource, File dest) throws IOException {
            String sha256Hash = null;
            if (dest.exists()) {
                LOGGER.debug("File \"{}\" already exists. It will be replaced.", dest);
            }
            DatumWriter<HttpSample> httpSampleDatumWriter = new SpecificDatumWriter<>(HttpSample.class);
            DatumReader<CsvJtlRow> httpSampleDatumReader = new SpecificDatumReader<>(CsvJtlRow.class);
            try (HashingOutputStream hos = new HashingOutputStream(Hashing.sha256(), new BufferedOutputStream(new FileOutputStream(dest)))) {
                try (DataFileWriter<HttpSample> dataFileWriter = new DataFileWriter<>(httpSampleDatumWriter);
                        DataFileReader<CsvJtlRow> reader = new DataFileReader<>(intermediateSource, httpSampleDatumReader)) {
                    dataFileWriter.setMeta("earliest", earliest);
                    dataFileWriter.setMeta("latest", latest);
                    dataFileWriter.setMeta("numRows", numRows);

                    if (!labels.isEmpty()) {
                        writeMetaStringArray(dataFileWriter, "labels", labels);
                    }

                    if (!threadNames.isEmpty()) {
                        writeMetaStringArray(dataFileWriter, "threadNames", threadNames);
                    }

                    if (!urls.isEmpty()) {
                        writeMetaStringArray(dataFileWriter, "urls", urls);
                    }

                    List<CharSequence> codes = statusCodeLookup.getCustomCodes();
                    List<CharSequence> messages = statusCodeLookup.getCustomMessages();
                    if (codes != null && !codes.isEmpty()) {
                        writeMetaStringArray(dataFileWriter, "codes", codes);
                        writeMetaStringArray(dataFileWriter, "messages", messages);
                    }
                    dataFileWriter.create(HttpSample.getClassSchema(), hos, SYNC);

                    Map<CharSequence, Integer> labelLookup = createLookup(labels);
                    Map<CharSequence, Integer> threadNameLookup = createLookup(threadNames);
                    Map<CharSequence, Integer> urlLookup = createLookup(urls);
                    long numRowsWritten = 0;
                    long writeStartMs = System.currentTimeMillis();
                    while (reader.hasNext()) {
                        for (long i = 0; i < 1000000L && reader.hasNext(); ++i) {
                            CsvJtlRow row = reader.next();
                            HttpSample httpSample = convert(row, labelLookup, threadNameLookup, urlLookup);
                            dataFileWriter.append(httpSample);
                            ++numRowsWritten;
                        }
                        LOGGER.debug("{}ms to write {} of {} rows.",
                                System.currentTimeMillis() - writeStartMs,
                                numRowsWritten, numRows);
                    }
                }
                sha256Hash = hos.hash().toString();
            }
            return sha256Hash;
        }

        // This iterable better not change between when this is called and when
        // the array is made, otherwise it'll be all sorts of messed up and you
        // won't be able to know.
        private static Map<CharSequence, Integer> createLookup(Iterable<CharSequence> items) {
            Map<CharSequence, Integer> lookup = new HashMap<>();
            Integer ref = 1;
            for (CharSequence item : items) {
                lookup.put(new Utf8(item.toString()), ref);
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

        private HttpSample convert(CsvJtlRow row,
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

    private static HttpSample createNewEmptyHttpSample() {
        HttpSample hs = new HttpSample();
        hs.setMillisElapsed(-1L);
        hs.setResponseBytes(-1L);
        return hs;
    }
}
