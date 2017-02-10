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
import com.redsaz.meterrier.importer.model.Entry;
import com.redsaz.meterrier.importer.model.HttpSample;
import com.redsaz.meterrier.importer.model.Metadata;
import com.redsaz.meterrier.importer.model.StringArray;
import com.redsaz.meterrier.importer.model.jmeter.CsvJtlRow;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    @Override
    public void convert(File source, File dest) {
        oldConvert(source, new File("jtls/real-columntrimmed.csv"));
        long startMillis = System.currentTimeMillis();
        long totalRows = 0;
        LOGGER.debug("Converting {} to {}...", source, dest);
        try {
            File intermediateData = new File("jtls/real-intermediate.avro");
            IntermediateInfo info = csvToIntermediate(source, intermediateData);
            info.writeAvro(intermediateData, dest);
            totalRows = info.numRows;
        } catch (RuntimeException | IOException ex) {
            throw new AppServerException("Unable to process import.", ex);
        }
        LOGGER.debug("{}ms to convert {} rows.", (System.currentTimeMillis() - startMillis), totalRows);
    }

    public void oldConvert(File source, File dest) {
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

    private IntermediateInfo csvToIntermediate(File source, File dest) throws IOException {
        IntermediateInfo info = new IntermediateInfo();
        BufferedReader br = new BufferedReader(new FileReader(source));
        CSVReader reader = new CSVReader(br);
        Iterator<String[]> csvIter = reader.iterator();
        List<JtlType> cols;
        if (csvIter.hasNext()) {
            String[] headers = csvIter.next();
            cols = new ArrayList<>(headers.length);
            for (String header : headers) {
                cols.add(JtlType.fromHeader(header));
            }
        } else {
            throw new RuntimeException("No headers defined.");
        }

        DatumWriter<CsvJtlRow> userDatumWriter = new SpecificDatumWriter<>(CsvJtlRow.class);
        try (DataFileWriter<CsvJtlRow> dataFileWriter = new DataFileWriter<>(userDatumWriter)) {
            dataFileWriter.create(CsvJtlRow.getClassSchema(), dest);
            while (csvIter.hasNext()) {
                String[] row = csvIter.next();
                CsvJtlRow out = new CsvJtlRow();
                for (int i = 0; i < row.length; ++i) {
                    String colVal = row[i];
                    JtlType colType = cols.get(i);
                    colType.putIn(out, colVal);
                }
                info.update(out);
                dataFileWriter.append(out);
            }
        }
        return info;
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
                    Entry labelEntry = new Entry(new StringArray("labels", new ArrayList<CharSequence>(labels)));
                    dataFileWriter.append(labelEntry);
                }
                Map<CharSequence, Integer> threadNameLookup = createLookup(threadNames);
                if (!threadNames.isEmpty()) {
                    Entry threadNamesEntry = new Entry(new StringArray("threadNames", new ArrayList<CharSequence>(threadNames)));
                    dataFileWriter.append(threadNamesEntry);
                }
                Map<CharSequence, Integer> urlLookup = createLookup(urls);
                if (!urls.isEmpty()) {
                    Entry urlsEntry = new Entry(new StringArray("urls", new ArrayList<CharSequence>(urls)));
                    dataFileWriter.append(urlsEntry);
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
            hs.setBytesReceived(longOrDefault(row.getBytes(), -1));
            hs.setBytesSent(longOrDefault(row.getSentBytes(), -1));
            hs.setCurrentThreads(intOrDefault(row.getAllThreads(), 0));
            int labelRef = labelLookup.getOrDefault(row.getLabel(), 0);
            if (labelRef < 1) {
                System.out.println("Huh? labelRef=" + labelRef);
            }
            hs.setLabelRef(labelRef);
            hs.setMillisElapsed(longOrDefault(row.getElapsed(), 0));
            hs.setMillisOffset(row.getTimeStamp() - earliest);
            hs.setResponseCodeRef(statusCodeLookup.getRef(row.getResponseCode()));
            hs.setSuccess(booleanOrDefault(row.getSuccess(), true));
            hs.setThreadNameRef(threadNameLookup.getOrDefault(row.getThreadName(), 0));
            hs.setUrlRef(urlLookup.getOrDefault(row.getURL(), 0));

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
        hs.setBytesReceived(-1L);
        hs.setBytesSent(-1L);
        return hs;
    }
}
