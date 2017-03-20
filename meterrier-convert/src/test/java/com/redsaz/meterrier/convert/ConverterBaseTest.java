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

import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.redsaz.meterrier.convert.model.Entry;
import com.redsaz.meterrier.convert.model.HttpSample;
import com.redsaz.meterrier.convert.model.Metadata;
import com.redsaz.meterrier.convert.model.StringArray;
import difflib.DiffUtils;
import difflib.Patch;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * @author Redsaz <redsaz@gmail.com>
 */
public class ConverterBaseTest {

    private static ThreadLocal<File> tempTestMethodDir = new ThreadLocal<File>() {
        @Override
        protected File initialValue() {
            try {
                File temp = new File(
                        File.createTempFile("test", ".md").getParentFile(),
                        "test-dir-" + Math.random() * 0x7FFFFFFF);
                boolean success = temp.mkdirs();
                if (!success) {
                    throw new IOException("Could not create temp dir " + tempTestMethodDir);
                }
                return temp;
            } catch (IOException ex) {
                throw new RuntimeException("Could not create temp directory.");
            }
        }
    };

    @BeforeMethod
    public void createTempFolder() {
        tempTestMethodDir.get();
    }

    @AfterMethod
    public void deleteTempFolder() {
        if (tempTestMethodDir.get().exists()) {
            recurseDelete(tempTestMethodDir.get());
        }
    }

    public File getTempFolder() {
        return tempTestMethodDir.get();
    }

    public File createTempFile(String prefix, String suffix) {
        try {
            return File.createTempFile(prefix, suffix, getTempFolder());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void assertContentEquals(File actual, File expected, String message) {
        try {
            List<String> expectedLines = Files.readAllLines(expected.toPath());
            List<String> actualLines = Files.readAllLines(actual.toPath());
            Patch<String> patch = DiffUtils.diff(expectedLines, actualLines);
            if (!patch.getDeltas().isEmpty()) {
                List<String> diff = DiffUtils.generateUnifiedDiff(expected.getPath(), actual.getPath(), expectedLines, patch, 3);
                StringBuilder sb = new StringBuilder(message);
                sb.append(" File contents do not match.\n");
                diff.stream().forEach((diffLine) -> {
                    sb.append(diffLine).append("\n");
                });
                fail(sb.toString());
            }
        } catch (IOException ex) {
            fail(ex.getMessage(), ex);
        }
    }

    public static void assertAvroContentEquals(File actual, File expected, String message) {
        DatumReader<Entry> userDatumReader = new SpecificDatumReader<>(Entry.class);
        try (DataFileReader<Entry> expectedReader = new DataFileReader<>(expected, userDatumReader);
                DataFileReader<Entry> actualReader = new DataFileReader<>(actual, userDatumReader)) {
            Iterator<Entry> eIter = expectedReader.iterator();
            Iterator<Entry> aIter = actualReader.iterator();
            while (eIter.hasNext() && aIter.hasNext()) {
                assertEntryEquals(aIter.next(), eIter.next(), message);
            }
        } catch (RuntimeException | IOException ex) {
            throw new AppServerException("Unable to convert file.", ex);
        }
    }

    private static void recurseDelete(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                recurseDelete(file);
            } else {
                file.delete();
            }
        }
    }

    private static void assertEntryEquals(Entry actual, Entry expect, String message) {
        Object eItem = expect.getItem();
        Object aItem = actual.getItem();

        assertEquals(aItem.getClass(), eItem.getClass(), message + " Entry item classes are not the same.");

        if (eItem instanceof HttpSample) {
            HttpSample eHs = (HttpSample) eItem;
            HttpSample aHs = (HttpSample) aItem;
            assertEquals(aHs.getBytesReceived(), eHs.getBytesReceived(), message + " Bytes Received not equal.");
            assertEquals(aHs.getCurrentThreads(), eHs.getCurrentThreads(), message + " Current Threads not equal.");
            assertEquals(aHs.getLabelRef(), eHs.getLabelRef(), message + " Label Ref not equal.");
            assertEquals(aHs.getMillisElapsed(), eHs.getMillisElapsed(), message + " Millis elapsed not equal.");
            assertEquals(aHs.getMillisOffset(), eHs.getMillisOffset(), message + " Millis offset not equal.");
            assertEquals(aHs.getResponseCodeRef(), eHs.getResponseCodeRef(), message + " Response Code Ref not equal.");
            assertEquals(aHs.getSuccess(), eHs.getSuccess(), message + " Success not equal.");
            assertEquals(aHs.getThreadNameRef(), eHs.getThreadNameRef(), message + " Thread Name Ref not equal.");
        } else if (eItem instanceof StringArray) {
            StringArray eSa = (StringArray) eItem;
            StringArray aSa = (StringArray) aItem;
            assertEquals(aSa.getName(), eSa.getName(), message + " Name not equal.");
            assertEquals(aSa.getValues(), eSa.getValues(), message + " Values not equal.");
        } else if (eItem instanceof Metadata) {
            Metadata em = (Metadata) eItem;
            Metadata am = (Metadata) aItem;
            assertEquals(am.getEarliestMillisUtc(), em.getEarliestMillisUtc(), message + " Earliest millis UTC not equal.");
            assertEquals(am.getLatestMillisUtc(), em.getLatestMillisUtc(), message + " Latest millis UTC not equal.");
            assertEquals(am.getTotalEntries(), em.getTotalEntries(), message + " Total entries not equal.");
        } else {
            fail("Unknown class: " + eItem.getClass());
        }
    }
}
