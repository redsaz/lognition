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
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Determines if the CSV file is a JMeter JTL file or a LoadyMcLoadface result log file and loads
 * the samples accordingly.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvAutoSource {

  private static final Logger LOG = LoggerFactory.getLogger(CsvAutoSource.class);

  private interface IdentifierByHeader {
    /** True if the sourcetype can process CSVs with this set of headers. */
    boolean identifiedByHeaders(List<String> headers);
  }

  private interface SamplesLoader {
    /** Loads the samples from the given file. */
    Samples load(File file);
  }

  /** List of CSV source types that can be auto-detected. */
  public enum CsvSourceType implements IdentifierByHeader, SamplesLoader {
    LOADY {
      private List<String> mustHaves =
          List.of(
              "completed_at_ms",
              "duration_ms",
              // Not required             "fail",
              "status",
              "bytes_up",
              // Not required             "bytes_down",
              // Not required             "call",
              "label",
              "thread");

      @Override
      public boolean identifiedByHeaders(List<String> headers) {
        return headers.containsAll(mustHaves);
      }

      @Override
      public Samples load(File file) {
        return CsvLoadySource.readLoadyFile(file);
      }
    },
    JTL {
      @Override
      public boolean identifiedByHeaders(List<String> headers) {
        if (headers.isEmpty()) {
          // No headers means that the file is empty, which can be handled by CsvJtlSource.
          return true;
        }
        String[] headerArr = headers.toArray(new String[headers.size()]);
        return HeaderCheckUtil.isJtlHeaderRow(headerArr)
            || HeaderCheckUtil.canUseDefaultHeaderRow(headerArr);
      }

      @Override
      public Samples load(File file) {
        return CsvJtlSource.readJtlFile(file);
      }
    }
  }

  // Do not instantiate utility classes.
  private CsvAutoSource() {}

  public static Samples loadSamples(File csvFile) {
    List<String> headers = readCsvHeaders(csvFile);
    return Arrays.stream(CsvSourceType.values())
        .filter(type -> type.identifiedByHeaders(headers))
        .findFirst()
        .orElseThrow(() -> new AppServerException("No converter found for \"" + csvFile + "\""))
        .load(csvFile);
  }

  private static List<String> readCsvHeaders(File source) {
    try (BufferedReader br = new BufferedReader(new FileReader(source))) {
      CsvParserSettings settings = new CsvParserSettings();
      CsvParser parser = new CsvParser(settings);
      parser.beginParsing(br);
      String[] row = parser.parseNext();
      if (row == null) {
        // header row is null if file is empty, which should be handled.
        return List.of();
      }
      return List.copyOf(Arrays.asList(row));
    } catch (IOException ex) {
      throw new AppServerException("Could not read headers from file \"" + source + "\"", ex);
    }
  }
}
