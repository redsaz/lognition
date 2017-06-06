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
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;

/**
 * Used to simulate performance data.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class MockPerfData {

    private final long earliest;
    private final long numRows;
    private final List<CharSequence> labels;
    private final List<CharSequence> threadNames;
    private final List<CharSequence> codes;
    private final List<CharSequence> messages;
    // Because we don't care about syncing, but DO care about repeatably
    // creating the same output data given the same input data, we'll use
    // our own sync marker rather than the randomly generated one that avro
    // gives us.
    private static final byte[] SYNC = new byte[16];

    public MockPerfData(
            long inEarliest,
            long inNumRows,
            List<CharSequence> inLabels,
            List<CharSequence> inThreadNames,
            List<CharSequence> inCodes,
            List<CharSequence> inMessages) {
        earliest = inEarliest;
        numRows = inNumRows;
        labels = inLabels;
        threadNames = inThreadNames;
        codes = inCodes;
        messages = inMessages;
    }

    public long getEarliest() {
        return earliest;
    }

    public long getLatest() {
        HttpSample hs = getRow(numRows - 1);
        return getEarliest() + hs.getMillisOffset() + hs.getMillisElapsed();
    }

    public long getNumRows() {
        return numRows;
    }

    public List<CharSequence> getLabels() {
        return labels;
    }

    public List<CharSequence> getThreadNames() {
        return threadNames;
    }

    public List<CharSequence> getCodes() {
        return codes;
    }

    public List<CharSequence> getMessages() {
        return messages;
    }

    public HttpSample getRow(long index) {
        long row = index + 1;
        HttpSample hs = new HttpSample();
        hs.setResponseBytes(row * 101L);
        hs.setTotalThreads(threadNames.size());
        hs.setLabelRef(((int) index % labels.size()) + 1);
        hs.setMillisElapsed(row * 307L);
        hs.setMillisOffset(60_000L / numRows * index);
        hs.setResponseCodeRef((int) (-64L - (index % codes.size())));
        hs.setSuccess(index % 2 == 0);
        hs.setThreadNameRef((int) (index % threadNames.size()) + 1);
        return hs;
    }

    public String createAvroFile(File dest) {
        String sha256Hash = null;
        DatumWriter<HttpSample> httpSampleDatumWriter = new SpecificDatumWriter<>(HttpSample.class);
        try (HashingOutputStream hos = new HashingOutputStream(Hashing.sha256(), new BufferedOutputStream(new FileOutputStream(dest)))) {
            try (DataFileWriter<HttpSample> dataFileWriter = new DataFileWriter<>(httpSampleDatumWriter)) {
                dataFileWriter.setMeta("earliest", getEarliest());
                dataFileWriter.setMeta("latest", getLatest());
                dataFileWriter.setMeta("numRows", getNumRows());

                writeMetaStringArray(dataFileWriter, "labels", getLabels());
                writeMetaStringArray(dataFileWriter, "threadNames", getThreadNames());
                writeMetaStringArray(dataFileWriter, "codes", getCodes());
                writeMetaStringArray(dataFileWriter, "messages", getMessages());

                dataFileWriter.create(HttpSample.getClassSchema(), hos, SYNC);

                for (int i = 0; i < getNumRows(); ++i) {
                    HttpSample hs = getRow(i);
                    dataFileWriter.append(hs);
                }
            }
            sha256Hash = hos.hash().toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return sha256Hash;
    }

    public String createExportedCsvFile(File dest) {
        String sha256Hash = null;
        StatusCodeLookup scl = new StatusCodeLookup(getCodes(), getMessages());
        try (HashingOutputStream hos = new HashingOutputStream(Hashing.sha256(), new BufferedOutputStream(new FileOutputStream(dest)))) {
            try (Writer w = new OutputStreamWriter(hos);
                    PrintWriter pw = new PrintWriter(w)) {
                pw.println("timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,bytes,allThreads");

                for (int i = 0; i < getNumRows(); ++i) {
                    HttpSample hs = getRow(i);
                    pw.printf("%d,%d,%s,%s,%s,%s,%s,%d,%d\n",
                            hs.getMillisOffset() + getEarliest(),
                            hs.getMillisElapsed(),
                            getLabels().get(hs.getLabelRef() - 1),
                            scl.getCode(hs.getResponseCodeRef()),
                            scl.getMessage(hs.getResponseCodeRef()),
                            getThreadNames().get(hs.getThreadNameRef() - 1),
                            hs.getSuccess(),
                            hs.getResponseBytes(),
                            hs.getTotalThreads()
                    );

                }
            }
            sha256Hash = hos.hash().toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return sha256Hash;
    }

    public void createImportCsvFile(File dest, boolean includeHeader) {
        StatusCodeLookup scl = new StatusCodeLookup(getCodes(), getMessages());
        try (BufferedWriter bw = Files.newBufferedWriter(dest.toPath());
                PrintWriter pw = new PrintWriter(bw)) {
            if (includeHeader) {
                pw.println("timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
            }

            for (int i = 0; i < getNumRows(); ++i) {
                HttpSample hs = getRow(i);
                pw.printf("%d,%d,%s,%s,%s,%s,%s,%s,%d,%d,%d,%d\n",
                        hs.getMillisOffset() + getEarliest(),
                        hs.getMillisElapsed(),
                        getLabels().get(hs.getLabelRef() - 1),
                        scl.getCode(hs.getResponseCodeRef()),
                        scl.getMessage(hs.getResponseCodeRef()),
                        getThreadNames().get(hs.getThreadNameRef() - 1),
                        "text",
                        hs.getSuccess(),
                        hs.getResponseBytes(),
                        hs.getTotalThreads(),
                        hs.getTotalThreads(),
                        0
                );
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void createImportCsvFileUnordered(File dest, boolean includeHeader) {
        StatusCodeLookup scl = new StatusCodeLookup(getCodes(), getMessages());
        try (BufferedWriter bw = Files.newBufferedWriter(dest.toPath());
                PrintWriter pw = new PrintWriter(bw)) {
            if (includeHeader) {
                pw.println("timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
            }

            List<HttpSample> samples = new ArrayList<HttpSample>();
            for (int i = 0; i < getNumRows(); ++i) {
                HttpSample hs = getRow(i);
                samples.add(hs);
            }
            Collections.shuffle(samples);
            for (HttpSample hs : samples) {
                pw.printf("%d,%d,%s,%s,%s,%s,%s,%s,%d,%d,%d,%d\n",
                        hs.getMillisOffset() + getEarliest(),
                        hs.getMillisElapsed(),
                        getLabels().get(hs.getLabelRef() - 1),
                        scl.getCode(hs.getResponseCodeRef()),
                        scl.getMessage(hs.getResponseCodeRef()),
                        getThreadNames().get(hs.getThreadNameRef() - 1),
                        "text",
                        hs.getSuccess(),
                        hs.getResponseBytes(),
                        hs.getTotalThreads(),
                        hs.getTotalThreads(),
                        0
                );
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
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

}
