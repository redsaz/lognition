package com.redsaz.lognition.convert;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

import com.redsaz.lognition.api.exceptions.AppServerException;
import com.redsaz.lognition.convert.model.HttpSample;
import difflib.DiffUtils;
import difflib.Patch;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;

public class TestUtil {

  // Do not instantiate utility classes.
  private TestUtil() {}

  public static List<String> stringLines(String stringWithNewLines) {
    try (BufferedReader br = new BufferedReader(new StringReader(stringWithNewLines))) {
      return br.lines().toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static void assertContentEquals(
      String actualLinesStr, String expectedLinesStr, String message) {
    List<String> actualLines = stringLines(actualLinesStr);
    List<String> expectedLines = stringLines(expectedLinesStr);
    Patch<String> patch = DiffUtils.diff(expectedLines, actualLines);
    if (!patch.getDeltas().isEmpty()) {
      List<String> diff =
          DiffUtils.generateUnifiedDiff("expected", "actual", expectedLines, patch, 3);
      StringBuilder sb = new StringBuilder(message);
      sb.append(" Contents do not match.\n");
      diff.stream()
          .forEach(
              (diffLine) -> {
                sb.append(diffLine).append("\n");
              });
      fail(sb.toString());
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
