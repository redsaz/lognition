/*
 * Copyright 2025 Redsaz <redsaz@gmail.com>.
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

import com.redsaz.lognition.api.exceptions.AppServerException;
import com.redsaz.lognition.api.model.Sample;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads Sample from a CSV-based LoadyMcLoadface results log.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvLoadySource {

  private static final Logger LOG = LoggerFactory.getLogger(CsvLoadySource.class);

  // Do not instantiate util class.
  private CsvLoadySource() {}

  public static Samples readLoadyFile(File source) {
    try {
      long startMillis = System.currentTimeMillis();
      LOG.info("Loading samples from LoadyMcLoadface results log \"{}\"", source);
      ListSamples.Builder builder = ListSamples.builder();
      readCsvFile(source, builder);
      Samples samples = builder.build();
      LOG.info(
          "Read {} rows in {}ms.",
          System.currentTimeMillis() - startMillis,
          samples.getSamples().size());
      return samples;
    } catch (RuntimeException | IOException ex) {
      throw new AppServerException("Unable to load from " + source, ex);
    }
  }

  private static void readCsvFile(File source, ListSamples.Builder builder) throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(source))) {
      CsvParserSettings settings = new CsvParserSettings();
      CsvParser parser = new CsvParser(settings);
      parser.beginParsing(br);
      String[] row = parser.parseNext();
      if (row == null) {
        LOG.debug("LoadyMcLoadface results log contained no data.");
        return;
      }
      LoadyTypeColumns jtc = new LoadyTypeColumns(row);
      while ((row = parser.parseNext()) != null) {
        Sample psRow = jtc.convert(row);
        if (psRow != null) {
          builder.add(psRow);
        }
      }
      // Loady has a constant number of threads for the entirety of its operation. So get count of
      // unique thread names, then adjust allThreads count.
      int numThreads = builder.getThreadNames().size();
      builder.forEach(sample -> sample.setTotalThreads(numThreads));
      parser.stopParsing();
    }
  }

  private static class LoadySample {
    long completedAtMs;
    long durationMs;
    int fail;
    String status;
    long bytesDown;
    String label = "";
    String thread;

    public Sample toSample() {
      Sample s = new Sample();
      s.setOffset(
          completedAtMs
              - durationMs); // offset is *start* of the call, not end like LoadyMcLoadface does.
      s.setDuration(durationMs);
      s.setLabel(label);
      s.setResponseBytes(bytesDown);
      s.setSuccess(fail == 0); // 0 means success (no fail of any type). Non-0 means fail.
      s.setStatusCode(status);
      s.setStatusMessage(""); // not captured by LoadyMcLoadface.
      s.setThreadName(thread);
      s.setTotalThreads(-1); // not captured by LoadyMcLoadface, it's constant, find later.

      return s;
    }
  }

  private enum LoadyType {
    COMPLETED_AT_MS("completed_at_ms", true) {
      @Override
      public void putIn(String src, LoadySample dest) {
        dest.completedAtMs = Long.parseLong(src);
      }
    },
    DURATION_MS("duration_ms", true) {
      @Override
      public void putIn(String src, LoadySample dest) {
        dest.durationMs = Long.parseLong(src);
      }
    },
    FAIL("fail", false) {
      @Override
      public void putIn(String src, LoadySample dest) {
        dest.fail = Integer.parseInt(src);
      }
    },
    STATUS("status", true) {
      @Override
      public void putIn(String src, LoadySample dest) {
        dest.status = src;
      }
    },
    BYTES_UP("bytes_up", false) {
      @Override
      public void putIn(String src, LoadySample dest) {
        // Unused, do nothing.
      }
    },
    BYTES_DOWN("bytes_down", true) {
      @Override
      public void putIn(String src, LoadySample dest) {
        dest.bytesDown = Long.parseLong(src);
      }
    },
    CALL("call", false) {
      @Override
      public void putIn(String src, LoadySample dest) {
        // Unused, do nothing.
      }
    },
    LABEL("label", false) {
      @Override
      public void putIn(String src, LoadySample dest) {
        dest.label = src;
      }
    },
    THREAD("thread", true) {
      @Override
      public void putIn(String src, LoadySample dest) {
        dest.thread = src;
      }
    };

    private final String header;
    private final boolean required;

    LoadyType(String header, boolean required) {
      this.header = header;
      this.required = required;
    }

    public abstract void putIn(String src, LoadySample dest);

    public String getHeader() {
      return header;
    }

    public boolean isRequired() {
      return required;
    }

    public static LoadyType fromHeader(String header) {
      return Arrays.stream(LoadyType.values())
          .filter(type -> type.header.equals(header))
          .findFirst()
          .orElse(null);
    }
  }

  private static class LoadyTypeColumns {

    private final List<LoadyType> colTypes;
    // Rather than have potentially a bunch of instances of identical strings, store previously
    // seen strings (rather than use String.intern()
    private final Map<String, String> stringPool = new HashMap<>();

    public LoadyTypeColumns(String[] header) {
      colTypes = new ArrayList<>(header.length);
      for (String headerCol : header) {
        LoadyType type = LoadyType.fromHeader(headerCol);
        if (type == null) {
          LOG.debug("Ignoring unknown header column \"{}\".", headerCol);
        }
        colTypes.add(type);
      }

      List<String> missingRequiredHeaders =
          Arrays.stream(LoadyType.values())
              .filter(LoadyType::isRequired)
              .filter(Predicate.not(colTypes::contains))
              .map(LoadyType::getHeader)
              .toList();

      if (!missingRequiredHeaders.isEmpty()) {
        String missingRequired =
            missingRequiredHeaders.stream().collect(Collectors.joining(", ", "\"", "\""));
        throw new IllegalArgumentException(
            "Required column(s) "
                + missingRequired
                + " missing from LoadyMcLoadface results file.");
      }
    }

    /**
     * Converts the row into a typed row. If the number of columns don't match the expected columns,
     * null is returned.
     *
     * @param row what to convert
     * @return a typed row, or null if it couldn't be converted.
     */
    public Sample convert(String[] row) {
      try {
        LoadySample out = new LoadySample();
        for (int i = 0; i < row.length; ++i) {
          String colVal = row[i];
          LoadyType colType = colTypes.get(i);
          if (colType != null) {
            colType.putIn(colVal, out);
          }
        }
        out.label = stringPoolerize(out.label);
        out.status = stringPoolerize(out.status);
        out.thread = stringPoolerize(out.thread);

        return out.toSample();
      } catch (RuntimeException ex) {
        LOG.debug(
            "Skipping bad row. Encountered {} when converting row. Contents:\n{}",
            ex.getMessage(),
            Arrays.toString(row));
        return null;
      }
    }

    // Like String.intern(), but not global
    private String stringPoolerize(String value) {
      if (value == null) {
        return null;
      }
      String old = stringPool.get(value);
      if (old == null) {
        stringPool.put(value, value);
        old = value;
      }
      return old;
    }
  }
}
