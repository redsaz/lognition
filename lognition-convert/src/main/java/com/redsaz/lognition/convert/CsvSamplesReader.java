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
  private CsvAutoSource.CsvSourceType sourceType;

  // Do not instantiate utility classes
  private CsvSamplesReader() {}

  public static Samples readSamples(Path file) throws IOException {
    CsvSamplesReader reader = new CsvSamplesReader();
    try (Stream<Sample> stream = Csvs.recordsUsing(file, reader::pickCsvDeserializer)) {
      ListSamples.Builder builder = ListSamples.builder();
      stream.forEach(builder::add);

      // JTL can have a varying total number of threads over time, but Loady is constant.
      // So for Loady, get count of unique thread names, then adjust allThreads count.
      if (reader.sourceType == CsvAutoSource.CsvSourceType.LOADY) {
        int numThreads = builder.getThreadNames().size();
        builder.forEach(sample -> sample.setTotalThreads(numThreads));
      }

      return builder.build();
    }
  }

  private Csvs.Deserializer<Sample> pickCsvDeserializer(List<String> headers) {
    this.sourceType =
        Arrays.stream(CsvAutoSource.CsvSourceType.values())
            .filter(type -> type.identifiedByHeaders(headers))
            .findFirst()
            .orElseThrow(
                () ->
                    new AppServerException(
                        "Cannot find Sample deserializer for headers: " + headers));

    return switch (this.sourceType) {
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
            case "success" -> (sample, row) -> sample.setSuccess(Boolean.parseBoolean(row[col]));
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
    // completed_at_ms,duration_ms,fail,status,bytes_up,bytes_down,call,label,thread
    final List<BiConsumer<LoadySample, String[]>> colConverters = new ArrayList<>(headers.size());
    for (int i = 0; i < headers.size(); ++i) {
      final int col = i;
      BiConsumer<LoadySample, String[]> action =
          switch (headers.get(col)) {
            case "completed_at_ms" ->
                (sample, row) -> sample.completedAtMs = Long.parseLong(row[col]);
            case "duration_ms" -> (sample, row) -> sample.durationMs = Long.parseLong(row[col]);
            case "fail" -> (sample, row) -> sample.fail = Integer.parseInt(row[col]);
            case "status" -> (sample, row) -> sample.status = row[col];
            case "bytes_down" -> (sample, row) -> sample.bytesDown = Long.parseLong(row[col]);
            case "label" -> (sample, row) -> sample.label = row[col];
            case "thread" -> (sample, row) -> sample.thread = row[col];
            default -> null;
          };
      if (action != null) {
        colConverters.add(action);
      }
    }
    return strings -> {
      LoadySample sample = new LoadySample();
      colConverters.forEach(c -> c.accept(sample, strings));
      return Stream.of(sample.toSample());
    };
  }

  private static class LoadySample {
    long completedAtMs;
    long durationMs;
    int fail;
    String status;
    long bytesDown;
    String label;
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
}
