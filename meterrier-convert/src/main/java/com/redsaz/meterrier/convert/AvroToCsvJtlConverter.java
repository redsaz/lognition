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

import com.opencsv.CSVWriter;
import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.redsaz.meterrier.convert.model.Entry;
import com.redsaz.meterrier.convert.model.HttpSample;
import com.redsaz.meterrier.convert.model.Metadata;
import com.redsaz.meterrier.convert.model.StringArray;
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
 * Convert an Avro file into a CSV-based JTL file.
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
            throw new AppServerException("Unable to convert file.", ex);
        }

        LOGGER.debug("{}ms to convert {} rows.", (System.currentTimeMillis() - startMillis), totalRows);
    }

    private static class HttpSampleToCsvJtl {

        private long earliestMillis = 0;
        private long latestMillis = 0;
        private StatusCodeLookup codes;
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
                List<CharSequence> customCodes = null;
                List<CharSequence> customMessages = null;
                while (dataFileReader.hasNext()) {
                    Entry entry = dataFileReader.next();
                    if (entry.getItem() instanceof HttpSample) {
                        HttpSample hs = (HttpSample) entry.getItem();
                        if (hs.getResponseCodeRef() != 0) {
                            usedFields.add(JtlType.RESPONSE_CODE);
                            usedFields.add(JtlType.RESPONSE_MESSAGE);
                        }
                        if (hs.getBytesReceived() != -1) {
                            usedFields.add(JtlType.BYTES);
                        }
                        if (hs.getCurrentThreads() > 0) {
                            usedFields.add(JtlType.ALL_THREADS);
                        }
                    } else if (entry.getItem() instanceof StringArray) {
                        StringArray sa = (StringArray) entry.getItem();
                        String name = sa.getName().toString();
                        if (null != name) {
                            switch (name) {
                                case "labels":
                                    labels = sa.getValues();
                                    usedFields.add(JtlType.LABEL);
                                    break;
                                case "threadNames":
                                    threadNames = sa.getValues();
                                    usedFields.add(JtlType.THREAD_NAME);
                                    break;
                                case "urls":
                                    urls = sa.getValues();
                                    usedFields.add(JtlType.URL);
                                    break;
                                case "codes":
                                    customCodes = sa.getValues();
                                    break;
                                case "messages":
                                    customMessages = sa.getValues();
                                    break;
                                default:
                                    LOGGER.warn("Unknown StringArray in file {}: {}", source, name);
                                    break;
                            }
                        }
                    } else if (entry.getItem() instanceof Metadata) {
                        Metadata md = (Metadata) entry.getItem();
                        earliestMillis = md.getEarliestMillisUtc();
                        latestMillis = md.getLatestMillisUtc();
                    }
                }
                codes = new StatusCodeLookup(customCodes, customMessages);
            } catch (RuntimeException | IOException ex) {
                throw new AppServerException("Unable to convert file.", ex);
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
            String[] result = new String[usedFields.size()];
            int index = 0;
            for (JtlType field : usedFields) {
                switch (field) {
                    case TIMESTAMP:
                        result[index] = Long.toString(earliestMillis + hs.getMillisOffset());
                        break;
                    case ELAPSED:
                        result[index] = hs.getMillisElapsed().toString();
                        break;
                    case LABEL:
                        result[index] = labels.get(hs.getLabelRef() - 1).toString();
                        break;
                    case RESPONSE_CODE:
                        result[index] = codes.getCode(hs.getResponseCodeRef()).toString();
                        break;
                    case RESPONSE_MESSAGE:
                        result[index] = codes.getMessage(hs.getResponseCodeRef()).toString();
                        break;
                    case THREAD_NAME:
                        result[index] = threadNames.get(hs.getThreadNameRef() - 1).toString();
                        break;
                    case SUCCESS:
                        result[index] = hs.getSuccess().toString();
                        break;
                    case BYTES:
                        result[index] = hs.getBytesReceived().toString();
                        break;
                    case ALL_THREADS:
                        result[index] = hs.getCurrentThreads().toString();
                        break;
                    default:
                        LOGGER.warn("Ignoring {} because convertsion to CSV form is not known.", field.csvName());
                        result[index] = null;
                }
                ++index;
            }

            return result;
        }
    }

}
