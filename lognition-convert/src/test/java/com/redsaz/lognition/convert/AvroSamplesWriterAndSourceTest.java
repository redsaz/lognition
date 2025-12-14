package com.redsaz.lognition.convert;

import static org.testng.Assert.assertEquals;

import com.redsaz.lognition.api.model.Sample;
import java.io.IOException;
import org.testng.annotations.Test;

public class AvroSamplesWriterAndSourceTest {

  @Test
  public void testWrite() throws IOException {
    // Test that samples that are written to file, result in the same values read from the file.
    try (TempFile temp = new TempFile()) {
      AvroSamplesWriter writer = new AvroSamplesWriter();
      Samples samples =
          ListSamples.builder()
              .add(Sample.of(1254L, 10L, "GET example/{id}", "1", "200", "OK", true, 123L, 2))
              .add(
                  Sample.of(
                      1367L,
                      12L,
                      "GET fail/{id}",
                      "1",
                      "NonHttpStatusCode",
                      "Connection Refused",
                      false,
                      123L,
                      2))
              .add(Sample.of(1607L, 20L, "GET example/{id}", "2", "200", "OK", true, 123L, 2))
              .build();
      writer.write(samples, temp.file());

      Samples result = AvroSamplesSource.loadFile(temp.path());
      assertEquals(result, samples);
    }
  }

  @Test
  public void testWriteUnorderedInputIsOrdered() throws IOException {
    // Test that samples that are written to file, result in the same values read from the file, but
    // ordered by sample starting timestamp.
    try (TempFile temp = new TempFile()) {
      AvroSamplesWriter writer = new AvroSamplesWriter();
      Samples samples =
          ListSamples.builder()
              .add(Sample.of(1607L, 20L, "GET example/{id}", "2", "200", "OK", true, 123L, 2))
              .add(
                  Sample.of(
                      1367L,
                      12L,
                      "GET fail/{id}",
                      "1",
                      "NonHttpStatusCode",
                      "Connection Refused",
                      false,
                      123L,
                      2))
              .add(Sample.of(1254L, 10L, "GET example/{id}", "1", "200", "OK", true, 123L, 2))
              .build();
      writer.write(samples, temp.file());

      Samples result = AvroSamplesSource.loadFile(temp.path());
      assertEquals(result.getSamples().get(0).getOffset(), 0L);
      assertEquals(result.getSamples().get(1).getOffset(), 113L);
      assertEquals(result.getSamples().get(2).getOffset(), 353L);
      assertEquals(result, samples);
    }
  }
}
