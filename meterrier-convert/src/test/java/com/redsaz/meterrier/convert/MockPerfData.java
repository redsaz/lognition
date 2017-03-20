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

import com.redsaz.meterrier.convert.model.Entry;
import com.redsaz.meterrier.convert.model.HttpSample;
import com.redsaz.meterrier.convert.model.Metadata;
import com.redsaz.meterrier.convert.model.StringArray;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
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

    public void createAvroFile(File dest) {
        DatumWriter<Entry> userDatumWriter = new SpecificDatumWriter<>(Entry.class);
        try (DataFileWriter<Entry> dataFileWriter = new DataFileWriter<>(userDatumWriter)) {
            dataFileWriter.create(Entry.getClassSchema(), dest);

            dataFileWriter.append(new Entry(new Metadata(getEarliest(), getLatest(), getNumRows())));

            Entry labelEntry = new Entry(new StringArray("labels", getLabels()));
            dataFileWriter.append(labelEntry);

            Entry threadNamesEntry = new Entry(new StringArray("threadNames", getThreadNames()));
            dataFileWriter.append(threadNamesEntry);

            Entry codesEntry = new Entry(new StringArray("codes", getCodes()));
            dataFileWriter.append(codesEntry);

            Entry messagesEntry = new Entry(new StringArray("messages", getMessages()));
            dataFileWriter.append(messagesEntry);

            for (int i = 0; i < getNumRows(); ++i) {
                HttpSample hs = getRow(i);
                Entry entry = new Entry(hs);
                dataFileWriter.append(entry);
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void createExportedCsvFile(File dest) {
        StatusCodeLookup scl = new StatusCodeLookup(getCodes(), getMessages());
        try (BufferedWriter bw = Files.newBufferedWriter(dest.toPath());
                PrintWriter pw = new PrintWriter(bw)) {
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
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
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

}
