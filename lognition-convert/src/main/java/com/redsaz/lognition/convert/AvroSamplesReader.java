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
import com.redsaz.lognition.convert.model.HttpSample;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.util.Utf8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads Samples from an Avro file.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class AvroSamplesReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(AvroSamplesReader.class);

  // Don't instantiate utility class.
  private AvroSamplesReader() {}

  public static Samples readSamples(Path avroFile) {
    long startMillis = System.currentTimeMillis();
    LOGGER.debug("Loading from Avro file {}...", avroFile);

    DatumReader<HttpSample> userDatumReader = new ReflectDatumReader<>(HttpSample.class);
    ListSamples.Builder samples = ListSamples.builder();
    try (DataFileReader<HttpSample> dataFileReader =
        new DataFileReader<>(avroFile.toFile(), userDatumReader)) {
      List<String> labels =
          readMetaStringArray(dataFileReader, "labels").stream()
              .map(CharSequence::toString)
              .toList();
      List<String> threadNames =
          readMetaStringArray(dataFileReader, "threadNames").stream()
              .map(CharSequence::toString)
              .toList();

      long absoluteStartTimestamp = dataFileReader.getMetaLong("earliest");
      List<CharSequence> customCodes = readMetaStringArray(dataFileReader, "codes");
      List<CharSequence> customMessages = readMetaStringArray(dataFileReader, "messages");
      StatusCodeLookup codes = new StatusCodeLookup(customCodes, customMessages);

      Function<HttpSample, Sample> conv =
          avroToSample(labels, threadNames, codes, absoluteStartTimestamp);
      while (dataFileReader.hasNext()) {
        HttpSample hs = dataFileReader.next();
        Sample sample = conv.apply(hs);
        samples.add(sample);
      }
    } catch (RuntimeException | IOException ex) {
      throw new AppServerException("Unable to convert file.", ex);
    }

    Samples result = samples.build();
    LOGGER.debug(
        "{}ms to convert {} rows.",
        (System.currentTimeMillis() - startMillis),
        result.getSamples().size());
    return result;
  }

  /**
   * Returns a stream of {@link Sample}s from an avro file.
   *
   * @apiNote Similar to {@link java.nio.file.Files#lines(Path)}, this should be used within a
   *     try-with-resources statement or similar to ensure the stream's file is closed promptly.
   * @param avroFile the file to read from
   * @return A CsvStream which has the headers and the stream to read the lines from.
   * @throws IOException if the file was not found or could not be opened.
   */
  public static Stream<Sample> sampleStream(Path avroFile) throws IOException {
    DatumReader<HttpSample> userDatumReader = new ReflectDatumReader<>(HttpSample.class);
    DataFileReader<HttpSample> dataFileReader =
        new DataFileReader<>(avroFile.toFile(), userDatumReader);
    List<String> labels =
        readMetaStringArray(dataFileReader, "labels").stream().map(CharSequence::toString).toList();
    List<String> threadNames =
        readMetaStringArray(dataFileReader, "threadNames").stream()
            .map(CharSequence::toString)
            .toList();

    long absoluteStartTimestamp = dataFileReader.getMetaLong("earliest");
    List<CharSequence> customCodes = readMetaStringArray(dataFileReader, "codes");
    List<CharSequence> customMessages = readMetaStringArray(dataFileReader, "messages");
    StatusCodeLookup codes = new StatusCodeLookup(customCodes, customMessages);

    Function<HttpSample, Sample> conv =
        avroToSample(labels, threadNames, codes, absoluteStartTimestamp);
    return StreamSupport.stream(dataFileReader.spliterator(), false)
        .map(conv)
        .onClose(uncheckedCloser(dataFileReader));
  }

  private static Function<HttpSample, Sample> avroToSample(
      List<String> labels,
      List<String> threadNames,
      StatusCodeLookup code,
      long absoluteStartTimestamp) {
    return (HttpSample hs) -> {
      Sample s = new Sample();
      s.setDuration(hs.getMillisElapsed());
      s.setLabel(labels.get(hs.getLabelRef() - 1));
      s.setOffset(hs.getMillisOffset() + absoluteStartTimestamp);
      s.setResponseBytes(hs.getResponseBytes());
      s.setStatusCode(code.getCode(hs.getResponseCodeRef()).toString());
      s.setStatusMessage(code.getMessage(hs.getResponseCodeRef()).toString());
      s.setSuccess(hs.getSuccess());
      s.setThreadName(threadNames.get(hs.getThreadNameRef() - 1).toString());
      s.setTotalThreads(hs.getTotalThreads());
      return s;
    };
  }

  private static List<CharSequence> readMetaStringArray(
      DataFileReader<?> dataFileReader, String name) throws IOException {
    List<CharSequence> items = null;
    byte[] buf = dataFileReader.getMeta(name);
    if (buf != null) {
      try (ByteArrayInputStream bais = new ByteArrayInputStream(buf)) {
        BinaryDecoder dec = DecoderFactory.get().directBinaryDecoder(bais, null);
        for (long i = dec.readArrayStart(); i > 0; i = dec.arrayNext()) {
          if (items == null) {
            items = new ArrayList<>((int) i);
          }
          for (long j = 0; j < i; j++) {
            Utf8 item = dec.readString(null);
            items.add(item);
          }
        }
      }
    }
    return items;
  }

  private static Runnable uncheckedCloser(Closeable closeable) {
    return () -> {
      try {
        closeable.close();
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    };
  }
}
