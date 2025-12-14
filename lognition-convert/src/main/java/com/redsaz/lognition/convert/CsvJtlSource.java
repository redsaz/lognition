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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads PreSamples from a CSV-based JTL source.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvJtlSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvJtlSource.class);

  private static final Set<JtlType> REQUIRED_COLUMNS =
      EnumSet.of(
          JtlType.TIMESTAMP,
          JtlType.ELAPSED,
          JtlType.LABEL,
          JtlType.RESPONSE_CODE,
          JtlType.THREAD_NAME,
          JtlType.SUCCESS,
          JtlType.BYTES,
          JtlType.ALL_THREADS);

  // Do not instantiate utility classes
  private CsvJtlSource() {}

  public static Samples readJtlFile(File source) {
    try {
      long startMillis = System.currentTimeMillis();
      LOGGER.debug("Loading samples from file {}...", source);
      ListSamples.Builder builder = ListSamples.builder();
      readCsvFile(source, builder);
      Samples samples = builder.build();
      LOGGER.debug(
          "...took {}ms to read {} rows.",
          System.currentTimeMillis() - startMillis,
          samples.getSamples().size());
      return samples;
    } catch (RuntimeException | IOException ex) {
      throw new AppServerException("Unable to convert file.", ex);
    }
  }

  private static void readCsvFile(File source, ListSamples.Builder builder) throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(source))) {
      CsvParserSettings settings = new CsvParserSettings();
      CsvParser parser = new CsvParser(settings);
      parser.beginParsing(br);
      String[] row = parser.parseNext();
      if (row == null) {
        LOGGER.debug("JTL (CSV) contained no data.");
        return;
      }
      JtlTypeColumns jtc = new JtlTypeColumns(row);
      if (jtc.headerAbsent()) {
        // If no header then the first row needs to be read as a regular sample.
        Sample psRow = jtc.convert(row);
        builder.add(psRow);
      }
      while ((row = parser.parseNext()) != null) {
        Sample psRow = jtc.convert(row);
        if (psRow != null) {
          builder.add(psRow);
        }
      }
      parser.stopParsing();
    }
  }

  private static class JtlTypeColumns {

    private List<JtlType> colTypes;
    private final boolean headerAbsent;
    // Rather than have potentially a bunch of instances of identical strings, store previously
    // seen strings (rather than use String.intern()
    private final Map<String, String> stringPool = new HashMap<>();

    public JtlTypeColumns(String[] header) {
      if (HeaderCheckUtil.isJtlHeaderRow(header)) {
        headerAbsent = false;
        colTypes = new ArrayList<>(header.length);
        for (String headerCol : header) {
          JtlType type = JtlType.fromHeader(headerCol);
          if (type == null) {
            LOGGER.warn("Ignoring unknown header column \"{}\".", headerCol);
          }
          colTypes.add(type);
        }
      } else {
        headerAbsent = true;
        if (HeaderCheckUtil.canUseDefaultHeaderRow(header)) {
          LOGGER.warn(
              "The JTL (CSV) file seems to be missing the header row. Using the expected defaults.");
          colTypes = HeaderCheckUtil.DEFAULT_HEADERS;
        } else {
          LOGGER.error(
              "No header row defined for JTL (CSV), and columns do not appear to be the defaults. Cannot convert.");
          throw new IllegalArgumentException(
              "Cannot convert from a JTL (CSV) with no header row and non-default column.");
        }
      }

      if (!colTypes.containsAll(REQUIRED_COLUMNS)) {
        throw new IllegalArgumentException(
            "Cannot convert from a JTL (CSV) when not all of the following columns are included: "
                + REQUIRED_COLUMNS);
      }
    }

    public boolean headerAbsent() {
      return headerAbsent;
    }

    /**
     * Converts the row into a typed row. If the number of columns don't match the expected columns,
     * null is returned.
     *
     * @param row what to convert
     * @return a typed row, or null if it couldn't be converted.
     */
    public Sample convert(String[] row) {
      if (row.length != colTypes.size()) {
        LOGGER.warn(
            "Skipping bad row. Expected {} columns but got {}. Contents:\n{}",
            colTypes.size(),
            row.length,
            Arrays.toString(row));
        return null;
      }
      try {
        Sample out = new Sample();
        for (int i = 0; i < row.length; ++i) {
          String colVal = row[i];
          JtlType colType = colTypes.get(i);
          if (colType != null) {
            colType.putIn(out, colVal);
          }
        }
        out.setLabel(stringPoolerize(out.getLabel()));
        out.setStatusCode(stringPoolerize(out.getStatusCode()));
        out.setStatusMessage(stringPoolerize(out.getStatusMessage()));
        out.setThreadName(stringPoolerize(out.getThreadName()));
        return out;
      } catch (NumberFormatException ex) {
        LOGGER.warn(
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
