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
package com.redsaz.lognition.convert;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

import com.redsaz.lognition.api.exceptions.AppServerException;
import com.redsaz.lognition.convert.model.HttpSample;
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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/** @author Redsaz <redsaz@gmail.com> */
public class ConverterBaseTest {

  private static ThreadLocal<File> tempTestMethodDir =
      new ThreadLocal<File>() {
        @Override
        protected File initialValue() {
          try {
            File temp =
                new File(
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

  public static void assertBytesEquals(File actual, File expected, String message) {
    try {
      byte[] expectedBytes = Files.readAllBytes(expected.toPath());
      byte[] actualBytes = Files.readAllBytes(actual.toPath());
      assertEquals(actualBytes.length, expectedBytes.length, "File length is incorrect.");
      for (int i = 0; i < expectedBytes.length; ++i) {
        assertEquals(
            actualBytes[i], expectedBytes[i], message + " Byte at pos=" + i + " is incorrect.");
      }
    } catch (IOException ex) {
      fail(ex.getMessage(), ex);
    }
  }

  public static void assertContentEquals(File actual, File expected, String message) {
    try {
      List<String> expectedLines = Files.readAllLines(expected.toPath());
      List<String> actualLines = Files.readAllLines(actual.toPath());
      Patch<String> patch = DiffUtils.diff(expectedLines, actualLines);
      if (!patch.getDeltas().isEmpty()) {
        List<String> diff =
            DiffUtils.generateUnifiedDiff(
                expected.getPath(), actual.getPath(), expectedLines, patch, 3);
        StringBuilder sb = new StringBuilder(message);
        sb.append(" File contents do not match.\n");
        diff.stream()
            .forEach(
                (diffLine) -> {
                  sb.append(diffLine).append("\n");
                });
        fail(sb.toString());
      }
    } catch (IOException ex) {
      fail(ex.getMessage(), ex);
    }
  }

  public static void assertAvroContentEquals(File actual, File expected, String message) {
    DatumReader<HttpSample> HttpSampleDatumReader = new SpecificDatumReader<>(HttpSample.class);
    try (DataFileReader<HttpSample> expectedReader =
            new DataFileReader<>(expected, HttpSampleDatumReader);
        DataFileReader<HttpSample> actualReader =
            new DataFileReader<>(actual, HttpSampleDatumReader)) {
      List<String> expectedMetaKeys = expectedReader.getMetaKeys();
      List<String> actualMetaKeys = actualReader.getMetaKeys();
      assertEquals(actualMetaKeys, expectedMetaKeys, "Metadata is incorrect.");
      for (String metaKey : expectedMetaKeys) {
        byte[] expectedValue = expectedReader.getMeta(metaKey);
        byte[] actualValue = actualReader.getMeta(metaKey);
        assertEquals(actualValue, expectedValue, "Value for key \"" + metaKey + "\" is incorrect.");
      }

      Iterator<HttpSample> eIter = expectedReader.iterator();
      Iterator<HttpSample> aIter = actualReader.iterator();
      while (eIter.hasNext() && aIter.hasNext()) {
        assertHttpSampleEquals(aIter.next(), eIter.next(), message);
      }
      assertFalse(eIter.hasNext(), "Actual content is missing entries.");
      assertFalse(aIter.hasNext(), "Actual content has too many entries.");
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

  private static void assertHttpSampleEquals(HttpSample actual, HttpSample expect, String message) {
    assertEquals(
        actual.getClass(), expect.getClass(), message + " Entry item classes are not the same.");

    if (expect instanceof HttpSample) {
      HttpSample eHs = (HttpSample) expect;
      HttpSample aHs = (HttpSample) actual;
      assertEquals(
          aHs.getResponseBytes(), eHs.getResponseBytes(), message + " Bytes Received not equal.");
      assertEquals(
          aHs.getTotalThreads(), eHs.getTotalThreads(), message + " Current Threads not equal.");
      assertEquals(aHs.getLabelRef(), eHs.getLabelRef(), message + " Label Ref not equal.");
      assertEquals(
          aHs.getMillisElapsed(), eHs.getMillisElapsed(), message + " Millis elapsed not equal.");
      assertEquals(
          aHs.getMillisOffset(), eHs.getMillisOffset(), message + " Millis offset not equal.");
      assertEquals(
          aHs.getResponseCodeRef(),
          eHs.getResponseCodeRef(),
          message + " Response Code Ref not equal.");
      assertEquals(aHs.getSuccess(), eHs.getSuccess(), message + " Success not equal.");
      assertEquals(
          aHs.getThreadNameRef(), eHs.getThreadNameRef(), message + " Thread Name Ref not equal.");
    } else {
      fail("Unknown class: " + expect.getClass());
    }
  }
}
