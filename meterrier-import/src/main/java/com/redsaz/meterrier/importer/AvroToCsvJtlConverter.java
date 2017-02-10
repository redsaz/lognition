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

import com.opencsv.CSVWriter;
import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.redsaz.meterrier.importer.model.Entry;
import com.redsaz.meterrier.importer.model.HttpSample;
import com.redsaz.meterrier.importer.model.Metadata;
import com.redsaz.meterrier.importer.model.StringArray;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert a CSV-based JTL file into an Avro file.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class AvroToCsvJtlConverter implements Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvroToCsvJtlConverter.class);

    @Override
    public void convert(File source, File dest) {
        long startMillis = System.currentTimeMillis();
        long totalRows = 0;
        LOGGER.debug("Converting {} to {}...", source, dest);
        HttpSampleToCsvJtl h2j = new HttpSampleToCsvJtl(source);

        DatumReader<Entry> userDatumReader = new SpecificDatumReader<>(Entry.class);
        try (DataFileReader<Entry> dataFileReader = new DataFileReader<>(source, userDatumReader);
                FileWriter fw = new FileWriter(dest);
                CSVWriter csvWriter = new CSVWriter(fw)) {
            csvWriter.writeNext(h2j.getUsedHeaders(), false);
            while (dataFileReader.hasNext()) {
                Entry entry = dataFileReader.next();
                if (entry.getItem() instanceof HttpSample) {
                    HttpSample hs = (HttpSample) entry.getItem();
                    String[] row = h2j.convert(hs);
                    csvWriter.writeNext(row, false);
                    ++totalRows;
                }
            }
        } catch (RuntimeException | IOException ex) {
            throw new AppServerException("Unable to process import.", ex);
        }

        LOGGER.debug("{}ms to convert {} rows.", (System.currentTimeMillis() - startMillis), totalRows);
    }

    private static class HttpSampleToCsvJtl {

        private long earliestMillis = 0;
        private long latestMillis = 0;
        private StatusCodeLookup codes = new StatusCodeLookup();
        private List<CharSequence> labels;
        private List<CharSequence> threadNames;
        private List<CharSequence> urls;
        private final EnumSet<JtlType> usedFields = EnumSet.of(JtlType.TIMESTAMP,
                JtlType.ELAPSED, JtlType.SUCCESS);

        HttpSampleToCsvJtl(File source) {
            long startMillis = System.currentTimeMillis();
            LOGGER.debug("Initializing converter for {}", source);
            DatumReader<Entry> userDatumReader = new SpecificDatumReader<>(Entry.class);
            try (DataFileReader<Entry> dataFileReader = new DataFileReader<>(source, userDatumReader)) {
                while (dataFileReader.hasNext()) {
                    Entry entry = dataFileReader.next();
                    if (entry.getItem() instanceof HttpSample) {
                        HttpSample hs = (HttpSample) entry.getItem();
                        if (hs.getResponseCodeRef() != 0) {
                            usedFields.add(JtlType.RESPONSE_CODE);
                        }
                        if (hs.getBytesReceived() != -1) {
                            usedFields.add(JtlType.BYTES);
                        }
                        if (hs.getBytesSent() != -1) {
                            usedFields.add(JtlType.SENT_BYTES);
                        }
                        if (hs.getCurrentThreads() > 0) {
                            usedFields.add(JtlType.ALL_THREADS);
                        }
                    } else if (entry.getItem() instanceof StringArray) {
                        StringArray sa = (StringArray) entry.getItem();
                        String name = sa.getName().toString();
                        LOGGER.info("Woo! Found {}!", name);
                        if ("labels".equals(name)) {
                            labels = sa.getValues();
                            usedFields.add(JtlType.LABEL);
                        } else if ("threadNames".equals(name)) {
                            threadNames = sa.getValues();
                            usedFields.add(JtlType.THREAD_NAME);
                        } else if ("urls".equals(name)) {
                            urls = sa.getValues();
                            usedFields.add(JtlType.URL);
                        }
                    } else if (entry.getItem() instanceof Metadata) {
                        Metadata md = (Metadata) entry.getItem();
                        earliestMillis = md.getEarliestMillisUtc();
                        latestMillis = md.getLatestMillisUtc();
                    }
                }
            } catch (RuntimeException | IOException ex) {
                throw new AppServerException("Unable to process import.", ex);
            }
            LOGGER.debug("Finished initializing converter in {}ms.", (System.currentTimeMillis() - startMillis));
        }

        public String[] getUsedHeaders() {
            String[] headers = new String[usedFields.size()];
            int index = 0;
            for (JtlType field : usedFields) {
                headers[index] = field.csvName();
                ++index;
            }
            return headers;
        }

        public String[] convert(HttpSample hs) {
            String[] result = new String[]{
                Long.toString(earliestMillis + hs.getMillisOffset()),
                hs.getMillisElapsed().toString(),
                labels.get(hs.getLabelRef() - 1).toString(),
                codes.getCode(hs.getResponseCodeRef()).toString(),
                threadNames.get(hs.getThreadNameRef() - 1).toString(),
                hs.getSuccess().toString(),
                hs.getBytesReceived().toString(),
                hs.getCurrentThreads().toString()};
            return result;
        }
    }

    private static class HttpSampleToCsvJtlOld {

        private long lastTimestamp = 0;
        private StatusCodeLookup codes = new StatusCodeLookup();
        private List<CharSequence> labels;
        private List<CharSequence> threadNames;
        private List<CharSequence> urls;
        private final EnumSet<JtlType> usedFields = EnumSet.of(JtlType.TIMESTAMP,
                JtlType.ELAPSED, JtlType.SUCCESS);

        HttpSampleToCsvJtlOld(File source) {
            long startMillis = System.currentTimeMillis();
            LOGGER.debug("Initializing converter for {}", source);
            DatumReader<Entry> userDatumReader = new SpecificDatumReader<>(Entry.class);
            try (DataFileReader<Entry> dataFileReader = new DataFileReader<>(source, userDatumReader)) {
                while (dataFileReader.hasNext()) {
                    Entry entry = dataFileReader.next();
                    if (entry.getItem() instanceof HttpSample) {
                        HttpSample hs = (HttpSample) entry.getItem();
                        if (hs.getResponseCodeRef() != 0) {
                            usedFields.add(JtlType.RESPONSE_CODE);
                        }
                        if (hs.getThreadNameRef() != 0) {
                            usedFields.add(JtlType.THREAD_NAME);
                        }
                        if (hs.getBytesReceived() != -1) {
                            usedFields.add(JtlType.BYTES);
                        }
                        if (hs.getBytesSent() != -1) {
                            usedFields.add(JtlType.SENT_BYTES);
                        }
                        if (hs.getCurrentThreads() > 0) {
                            usedFields.add(JtlType.ALL_THREADS);
                        }
                    } else if (entry.getItem() instanceof StringArray) {
                        StringArray sa = (StringArray) entry.getItem();
                        String name = sa.getName().toString();
                        LOGGER.info("Woo! Found {}!", name);
                        if ("labels".equals(name)) {
                            labels = sa.getValues();
                            usedFields.add(JtlType.LABEL);
                        } else if ("threadNames".equals(name)) {
                            threadNames = sa.getValues();
                            usedFields.add(JtlType.THREAD_NAME);
                        } else if ("urls".equals(name)) {
                            urls = sa.getValues();
                            usedFields.add(JtlType.URL);
                        }
                    }
                }
            } catch (RuntimeException | IOException ex) {
                throw new AppServerException("Unable to process import.", ex);
            }
            LOGGER.debug("Finished initializing converter in {}ms.", (System.currentTimeMillis() - startMillis));
        }

        public String[] getUsedHeaders() {
            String[] headers = new String[usedFields.size()];
            int index = 0;
            for (JtlType field : usedFields) {
                headers[index] = field.csvName();
                ++index;
            }
            return headers;
        }

        public String[] convert(HttpSample hs) {
            String[] result = new String[]{
                Long.toString(lastTimestamp + hs.getMillisOffset()),
                hs.getMillisElapsed().toString(),
                labels.get(hs.getLabelRef() - 1).toString(),
                codes.getCode(hs.getResponseCodeRef()).toString(),
                hs.getSuccess().toString(),
                hs.getBytesReceived().toString(),
                hs.getCurrentThreads().toString()};
            lastTimestamp += hs.getMillisOffset();
            return result;
        }
    }
}
