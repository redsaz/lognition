/*
 * Copyright 2026 Redsaz <redsaz@gmail.com>.
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

import com.redsaz.lognition.api.model.Sample;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.testng.annotations.Test;

/**
 * Test the Avro to CSV-JTL converter.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvJtlSamplesWriterTest {

  @Test
  public void testOutputStreamWriter() throws IOException {
    // Given an avro file,
    File source = new File("src/test/resources/test.avro");

    try (TempContent actualDest = TempContent.withName("actual", ".csv");
        Stream<Sample> samples = AvroSamplesReader.sampleStream(source.toPath());
        OutputStream out = Files.newOutputStream(actualDest.path())) {

      // When writing CSV-JTL output,
      CsvJtlSamplesWriter.outputStreamWriter(samples).accept(out);

      // Then the output should have the expected headers and content.
      String expected = Files.readString(Path.of("src/test/resources/test-expected-export.jtl"));
      assertEquals(
          actualDest.content(), expected, "The converter did not convert in the way expected.");
    }
  }

  @Test
  public void testOutputStreamWriter_emptyStream() throws IOException {
    // Given an empty stream of samples,
    Stream<Sample> samples = Stream.empty();

    // When writing a CSV-JTL output,
    try (TempContent actualDest = TempContent.withName("actual", ".csv");
        OutputStream out = Files.newOutputStream(actualDest.path())) {
      CsvJtlSamplesWriter.outputStreamWriter(samples).accept(out);

      // Then the result should be a CSV file with only headers (and a newline).
      String actual = actualDest.content();
      String expected =
          "timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,bytes,allThreads\n";
      assertEquals(actual, expected, "Only a header should result from an empty Sample stream.");
    }
  }
}
