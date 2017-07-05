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
import com.redsaz.meterrier.convert.model.HttpSample;
import com.redsaz.meterrier.convert.model.PreSample;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Writes samples to an Avro file.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class AvroSamplesWriter implements SamplesWriter {

    // Because we don't care about syncing, but DO care about repeatably
    // creating the same output data given the same input data, we'll use
    // our own sync marker rather than the randomly generated one that avro
    // gives us.
    private static final byte[] SYNC = new byte[16];

    private static final Logger LOGGER = LoggerFactory.getLogger(AvroSamplesWriter.class);

    @Override
    public String write(Samples sourceSamples, File dest) throws IOException {
        String sha256Hash = null;
        if (dest.exists()) {
            LOGGER.debug("File \"{}\" already exists. It will be replaced.", dest);
        }
        DatumWriter<HttpSample> httpSampleDatumWriter = new SpecificDatumWriter<>(HttpSample.class);
        List<CharSequence> labels = createSortedList(sourceSamples.getLabels());
        Map<CharSequence, Integer> labelLookup = createLookup(labels);
        List<CharSequence> threadNames = createSortedList(sourceSamples.getThreadNames());
        Map<CharSequence, Integer> threadNameLookup = createLookup(threadNames);
        try (HashingOutputStream hos = new HashingOutputStream(Hashing.sha256(), new BufferedOutputStream(new FileOutputStream(dest)))) {
            try (DataFileWriter<HttpSample> dataFileWriter = new DataFileWriter<>(httpSampleDatumWriter)) {
                dataFileWriter.setMeta("earliest", sourceSamples.getEarliestMillis());
                dataFileWriter.setMeta("latest", sourceSamples.getLatestMillis());
                dataFileWriter.setMeta("numRows", sourceSamples.getSamples().size());

                if (!labels.isEmpty()) {
                    writeMetaStringArray(dataFileWriter, "labels", labels);
                }

                if (!threadNames.isEmpty()) {
                    writeMetaStringArray(dataFileWriter, "threadNames", threadNames);
                }

                StatusCodeLookup statusCodeLookup = sourceSamples.getStatusCodeLookup();
                List<CharSequence> codes = statusCodeLookup.getCustomCodes();
                List<CharSequence> messages = statusCodeLookup.getCustomMessages();
                if (codes != null && !codes.isEmpty()) {
                    writeMetaStringArray(dataFileWriter, "codes", codes);
                    writeMetaStringArray(dataFileWriter, "messages", messages);
                }
                dataFileWriter.create(HttpSample.getClassSchema(), hos, SYNC);

                long numRowsWritten = 0;
                long writeStartMs = System.currentTimeMillis();
                for (PreSample presample : sourceSamples.getSamples()) {
                    HttpSample httpSample = convert(presample, labelLookup, threadNameLookup,
                            statusCodeLookup);
                    dataFileWriter.append(httpSample);
                    ++numRowsWritten;
                    if (numRowsWritten % 1000000L == 0) {
                        LOGGER.debug("{}ms to write {} of {} rows so far.",
                                System.currentTimeMillis() - writeStartMs,
                                numRowsWritten, sourceSamples.getSamples().size());
                    }
                }
            }
            sha256Hash = hos.hash().toString();
        }
        return sha256Hash;
    }

    private static HttpSample createNewEmptyHttpSample() {
        HttpSample hs = new HttpSample();
        hs.setMillisElapsed(-1L);
        hs.setResponseBytes(-1L);
        return hs;
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

    private static HttpSample convert(PreSample row,
            Map<CharSequence, Integer> labelLookup,
            Map<CharSequence, Integer> threadNameLookup,
            StatusCodeLookup statusCodeLookup) {
        HttpSample hs = createNewEmptyHttpSample();
        hs.setResponseBytes(longOrDefault(row.getResponseBytes(), -1));
        hs.setTotalThreads(intOrDefault(row.getTotalThreads(), 0));
        int labelRef = labelLookup.getOrDefault(row.getLabel(), 0);
        if (labelRef < 1) {
            LOGGER.warn("Bad labelRef={}", labelRef);
        }
        hs.setLabelRef(labelRef);
        hs.setMillisElapsed(longOrDefault(row.getDuration(), 0));
        hs.setMillisOffset(row.getOffset());
        hs.setResponseCodeRef(statusCodeLookup.getRef(row.getStatusCode(), row.getStatusMessage()));
        hs.setSuccess(booleanOrDefault(row.isSuccess(), true));
        hs.setThreadNameRef(threadNameLookup.getOrDefault(row.getThreadName(), 0));

        return hs;
    }

    private static long longOrDefault(Long value, long defaultVal) {
        if (value == null) {
            return defaultVal;
        }
        return value;
    }

    private static int intOrDefault(Integer value, int defaultVal) {
        if (value == null) {
            return defaultVal;
        }
        return value;
    }

    private static boolean booleanOrDefault(Boolean value, boolean defaultVal) {
        if (value == null) {
            return defaultVal;
        }
        return value;
    }

}
