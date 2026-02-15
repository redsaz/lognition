package com.redsaz.lognition.convert;

import com.redsaz.lognition.api.exceptions.AppServerException;
import com.redsaz.lognition.api.model.Sample;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Reads performance Samples from a CSV file, currently only JMeter and Loady McLoadface CSV result
 * files are supported automatically.
 */
public class CsvSamplesReader {

  public static void main(String[] args) throws IOException {
    Samples samples = readSamples(Path.of("/home/shayne/code/lognition/jtls/target/real-smaller-with-alternate-header.jtl"));
    System.out.println(samples);
  }

  // Do not instantiate utility classes
  private CsvSamplesReader() { }

//  THIS NEEDS TO BE CALLED! SOMEWHERE!  THEN MAYBE WE CAN REMOVE THE UNNEEDED PARTS FROM CSVAUTOSOURCE AND
//  BRING THE REST INTO HERE! AND GET RID OF CsvLoadySource and CsvAutoSource!
  public static Samples readSamples(Path file) throws IOException {
    TabStream stream = Csvs.recordsAsStrings(file);
    ListSamples.Builder builder = ListSamples.builder();
    stream.stream().map(schemaToConverter(file, stream.schema())).forEach(builder::add);
    return builder.build();
  }

  private static Function<? super TabRecord, Sample> schemaToConverter(Path file, TabSchema.StructS schema) {
    List<String> headers = schema.fields().stream().map(TabSchema::name).toList();

    CsvAutoSource.CsvSourceType srcType = Arrays.stream(CsvAutoSource.CsvSourceType.values())
        .filter(type -> type.identifiedByHeaders(headers))
        .findFirst()
        .orElseThrow(() -> new AppServerException("No converter found for \"" + file + "\""));

    return switch (srcType) {
      case JTL -> schema.converter(Sample.class);
      case LOADY -> schema.converter(LoadySample.class).andThen(LoadySample::toSample);
    };
  }

  private record LoadySample(long completedAtMs, long durationMs, int fail, String status, long bytesDown, String label, String thread) {
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

}
