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

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.redsaz.lognition.api.model.Sample;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes samples to a CSV-based Jmeter results log (JTL) file.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvJtlSamplesWriter implements SamplesWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvJtlSamplesWriter.class);

  @Override
  public String write(Samples sourceSamples, File dest) throws IOException {
    String sha256Hash = null;
    if (dest.exists()) {
      LOGGER.debug("File \"{}\" already exists. It will be replaced.", dest);
    }
    try (HashingOutputStream hos =
        new HashingOutputStream(
            Hashing.sha256(), new BufferedOutputStream(new FileOutputStream(dest)))) {
      try (BufferedWriter bw =
          new BufferedWriter(new OutputStreamWriter(hos, StandardCharsets.UTF_8))) {
        CsvWriter writer = null;
        try {
          writer = new CsvWriter(bw, new CsvWriterSettings());

          writer.writeHeaders(
              "timeStamp",
              "elapsed",
              "label",
              "responseCode",
              "responseMessage",
              "threadName",
              "success",
              "bytes",
              "allThreads");
          long originTimestamp = sourceSamples.getEarliestMillis();
          Function<Sample, Object[]> toCsv = sample -> toCsv(originTimestamp, sample);
          sourceSamples.getSamples().stream().map(toCsv).forEach(writer::writeRow);
        } finally {
          if (writer != null) {
            writer.close();
          }
        }
      }
      sha256Hash = hos.hash().toString();
    }
    return sha256Hash;
  }

  /**
   * Writes a stream of {@link Sample}s to an {@link OutputStream} in CSV JTL (Jmeter) form.
   *
   * @param samples The samples to write.
   * @return A function that takes an OutputStream and writes a CSV to it.
   */
  public static Consumer<OutputStream> outputStreamWriter(Stream<Sample> samples) {
    return (OutputStream os) -> writeSamples(samples, os);
  }

  private static void writeSamples(Stream<Sample> samples, OutputStream os) {
    try (BufferedWriter bw =
        new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
      CsvWriter writer = null; // CsvWriter doesn't implement Closeable, boo
      try {
        writer = new CsvWriter(bw, new CsvWriterSettings());

        writer.writeHeaders(
            "timeStamp",
            "elapsed",
            "label",
            "responseCode",
            "responseMessage",
            "threadName",
            "success",
            "bytes",
            "allThreads");
        Function<Sample, Object[]> toCsv =
            sample ->
                new Object[] {
                  sample.getOffset(),
                  sample.getDuration(),
                  sample.getLabel(),
                  sample.getStatusCode(),
                  sample.getStatusMessage(),
                  sample.getThreadName(),
                  sample.isSuccess(),
                  sample.getResponseBytes(),
                  sample.getTotalThreads()
                };
        samples.map(toCsv).forEach(writer::writeRow);
      } finally {
        if (writer != null) {
          writer.close();
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Object[] toCsv(long originTimestamp, Sample sample) {
    return new Object[] {
      originTimestamp + sample.getOffset(),
      sample.getDuration(),
      sample.getLabel(),
      sample.getStatusCode(),
      sample.getStatusMessage(),
      sample.getThreadName(),
      sample.isSuccess(),
      sample.getResponseBytes(),
      sample.getTotalThreads()
    };
  }
}
