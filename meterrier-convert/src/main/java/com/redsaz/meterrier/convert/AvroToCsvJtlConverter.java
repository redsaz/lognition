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
import com.opencsv.CSVWriter;
import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.redsaz.meterrier.convert.model.HttpSample;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.util.Utf8;
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
    public String convert(File source, File dest) {
        long startMillis = System.currentTimeMillis();
        long totalRows = 0;
        LOGGER.debug("Converting {} to {}...", source, dest);
        HttpSampleToCsvJtl h2j = new HttpSampleToCsvJtl(source);
        String sha256Hash = null;

        DatumReader<HttpSample> userDatumReader = new SpecificDatumReader<>(HttpSample.class);
        try (HashingOutputStream hos = new HashingOutputStream(Hashing.sha256(), new BufferedOutputStream(new FileOutputStream(dest)))) {
            try (DataFileReader<HttpSample> dataFileReader = new DataFileReader<>(source, userDatumReader);
                    OutputStreamWriter osw = new OutputStreamWriter(hos, Charset.forName("UTF8"));
                    CSVWriter csvWriter = new CSVWriter(osw)) {
                csvWriter.writeNext(h2j.getUsedHeaders(), false);
                while (dataFileReader.hasNext()) {
                    HttpSample hs = dataFileReader.next();
                    String[] row = h2j.convert(hs);
                    csvWriter.writeNext(row, false);
                    ++totalRows;
                }
            }
            sha256Hash = hos.hash().toString();
        } catch (RuntimeException | IOException ex) {
            throw new AppServerException("Unable to convert file.", ex);
        }

        LOGGER.debug("{}ms to convert {} rows.", (System.currentTimeMillis() - startMillis), totalRows);
        return sha256Hash;
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
            DatumReader<HttpSample> httpSampleDatumReader = new SpecificDatumReader<>(HttpSample.class);
            try (DataFileReader<HttpSample> dataFileReader = new DataFileReader<>(source, httpSampleDatumReader)) {
                earliestMillis = dataFileReader.getMetaLong("earliest");
                latestMillis = dataFileReader.getMetaLong("latest");
                labels = readMetaStringArray(dataFileReader, "labels");
                if (labels != null) {
                    usedFields.add(JtlType.LABEL);
                }
                threadNames = readMetaStringArray(dataFileReader, "threadNames");
                if (threadNames != null) {
                    usedFields.add(JtlType.THREAD_NAME);
                }
                urls = readMetaStringArray(dataFileReader, "urls");
                if (urls != null) {
                    usedFields.add(JtlType.URL);
                }
                List<CharSequence> customCodes = readMetaStringArray(dataFileReader, "codes");
                List<CharSequence> customMessages = readMetaStringArray(dataFileReader, "messages");
                codes = new StatusCodeLookup(customCodes, customMessages);
                while (dataFileReader.hasNext()) {
                    HttpSample hs = dataFileReader.next();
                    if (hs.getResponseCodeRef() != 0) {
                        usedFields.add(JtlType.RESPONSE_CODE);
                        usedFields.add(JtlType.RESPONSE_MESSAGE);
                    }
                    if (hs.getResponseBytes() != -1) {
                        usedFields.add(JtlType.BYTES);
                    }
                    if (hs.getTotalThreads() > 0) {
                        usedFields.add(JtlType.ALL_THREADS);
                    }
                }
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
                        result[index] = hs.getResponseBytes().toString();
                        break;
                    case ALL_THREADS:
                        result[index] = hs.getTotalThreads().toString();
                        break;
                    default:
                        LOGGER.warn("Ignoring {} because convertsion to CSV form is not known.", field.csvName());
                        result[index] = null;
                }
                ++index;
            }

            return result;
        }

        private static List<CharSequence> readMetaStringArray(DataFileReader<?> dataFileReader, String name) throws IOException {
            List<CharSequence> items = null;
            byte[] buf = dataFileReader.getMeta(name);
            if (buf != null) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(buf)) {
                    BinaryDecoder dec = DecoderFactory.get().directBinaryDecoder(bais, null);
                    for (long i = dec.readArrayStart(); i > 0; i = dec.arrayNext()) {
                        if (items == null) {
                            items = new ArrayList<>((int) i);
                        }
                        for (long j = 0; j < i; j++) {
                            Utf8 item = dec.readString(null);
                            items.add(item);
                        }
                    }
                }
            }
            return items;
        }
    }

}
