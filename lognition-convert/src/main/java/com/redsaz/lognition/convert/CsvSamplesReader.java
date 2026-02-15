package com.redsaz.lognition.convert;

import com.redsaz.lognition.api.exceptions.AppServerException;
import com.redsaz.lognition.api.model.Sample;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Reads performance Samples from a CSV file, currently only JMeter and Loady McLoadface CSV result
 * files are supported automatically.
 */
public class CsvSamplesReader {

  public static void main(String[] args) throws IOException {
    Path csvFile = Path.of("/home/shayne/code/lognition/jtls/target/bighuge.jtl");
    long startMs = System.currentTimeMillis();
    Samples samples = readSamples(csvFile);
    System.out.printf(
        "Loaded %d samples with CsvSamplesReader in %dms\n",
        samples.getSamples().size(), System.currentTimeMillis() - startMs);
    samples = null;

    startMs = System.currentTimeMillis();
    samples = CsvJtlSource.readJtlFile(csvFile.toFile());
    System.out.printf(
        "Loaded %d samples from CsvJtlSource in %dms\n",
        samples.getSamples().size(), System.currentTimeMillis() - startMs);
  }

  // Do not instantiate utility classes
  private CsvSamplesReader() {}

  //  THIS NEEDS TO BE CALLED! SOMEWHERE!  THEN MAYBE WE CAN REMOVE THE UNNEEDED PARTS FROM
  // CSVAUTOSOURCE AND
  //  BRING THE REST INTO HERE! AND GET RID OF CsvLoadySource and CsvAutoSource!
  public static Samples readSamples(Path file) throws IOException {
    try (Stream<Sample> stream = Csvs.recordsUsing(file, CsvSamplesReader::autoCsvDeserializer)) {
      ListSamples.Builder builder = ListSamples.builder();
      stream.forEach(builder::add);
      return builder.build();
    }
  }

  private static Csvs.Deserializer<Sample> autoCsvDeserializer(List<String> headers) {

    CsvAutoSource.CsvSourceType srcType =
        Arrays.stream(CsvAutoSource.CsvSourceType.values())
            .filter(type -> type.identifiedByHeaders(headers))
            .findFirst()
            .orElseThrow(
                () ->
                    new AppServerException(
                        "Cannot find Sample deserializer for headers: " + headers));

    return switch (srcType) {
      case JTL -> sampleMaker(headers);
      case LOADY -> fromLoadSampleMaker(headers);
    };
  }

  private static Csvs.Deserializer<Sample> sampleMaker(List<String> headers) {
    final List<BiConsumer<Sample, String[]>> colConverters = new ArrayList<>(headers.size());
    for (int i = 0; i < headers.size(); ++i) {
      final int col = i;
      BiConsumer<Sample, String[]> action =
          switch (headers.get(col)) {
            case "timeStamp" -> (sample, row) -> sample.setOffset(Long.parseLong(row[col]));
            case "elapsed" -> (sample, row) -> sample.setDuration(Long.parseLong(row[col]));
            case "label" -> (sample, row) -> sample.setLabel(row[col]);
            case "responseCode" -> (sample, row) -> sample.setStatusCode(row[col]);
            case "responseMessage" -> (sample, row) -> sample.setStatusMessage(row[col]);
            case "threadName" -> (sample, row) -> sample.setThreadName(row[col]);
            case "success" -> (sample, row) -> sample.setSuccess(Boolean.getBoolean(row[col]));
            case "bytes" -> (sample, row) -> sample.setResponseBytes(Long.parseLong(row[col]));
            case "allThreads" ->
                (sample, row) -> sample.setTotalThreads(Integer.parseInt(row[col]));
            default -> null;
          };
      if (action != null) {
        colConverters.add(action);
      }
    }
    return strings -> {
      Sample sample = new Sample();
      colConverters.forEach(c -> c.accept(sample, strings));
      return Stream.of(sample);
    };
  }

  private static Csvs.Deserializer<Sample> fromLoadSampleMaker(List<String> headers) {
    return null;
  }

  private record LoadySample(
      long completedAtMs,
      long durationMs,
      int fail,
      String status,
      long bytesDown,
      String label,
      String thread) {
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
