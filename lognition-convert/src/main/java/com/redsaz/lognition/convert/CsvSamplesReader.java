package com.redsaz.lognition.convert;

import com.redsaz.lognition.api.exceptions.AppServerException;
import com.redsaz.lognition.api.model.Sample;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads performance Samples from a CSV file, currently only JMeter and Loady McLoadface CSV result
 * files are supported automatically.
 */
public class CsvSamplesReader {
  private static final Logger LOG = LoggerFactory.getLogger(CsvSamplesReader.class);

  private CsvSourceType sourceType;

  // Do not instantiate utility classes
  private CsvSamplesReader() {}

  public static Samples readSamples(Path file) throws IOException {
    CsvSamplesReader reader = new CsvSamplesReader();
    try (Stream<Sample> stream = Csvs.recordsUsing(file, reader::pickCsvDeserializer)) {
      ListSamples.Builder builder = ListSamples.builder();
      stream.forEach(builder::add);

      // JTL can have a varying total number of threads over time, but Loady is constant.
      // So for Loady, get count of unique thread names, then adjust allThreads count.
      if (reader.sourceType == CsvSourceType.LOADY) {
        int numThreads = builder.getThreadNames().size();
        builder.forEach(sample -> sample.setTotalThreads(numThreads));
      }

      return builder.build();
    }
  }

  private Csvs.Deserializer<Sample> pickCsvDeserializer(List<String> headers) {
    this.sourceType =
        Arrays.stream(CsvSourceType.values())
            .filter(type -> type.identifiedByHeaders(headers))
            .findFirst()
            .orElseThrow(
                () ->
                    new AppServerException(
                        "Cannot find Sample deserializer for headers: " + headers));

    return this.sourceType.apply(headers);
  }

  /** List of CSV source types that can be auto-detected. */
  public enum CsvSourceType implements IdentifierByHeader, Csvs.DeserializerPlanner<Sample> {
    LOADY {
      @Override
      public Csvs.Deserializer<Sample> apply(List<String> headers) {
        int expectedColumns = headers.size();
        final List<BiConsumer<LoadySample, String[]>> colConverters =
            new ArrayList<>(headers.size());
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
                case "label" ->
                    (sample, row) -> sample.label = Objects.requireNonNullElse(row[col], "");
                case "thread" -> (sample, row) -> sample.thread = row[col];
                default -> null;
              };
          if (action != null) {
            colConverters.add(action);
          }
        }
        return strings -> {
          LoadySample sample = new LoadySample();
          if (strings.length != expectedColumns) {
            LOG.warn(
                "Expected {} values, but got {} instead. Skipping row.",
                expectedColumns,
                strings.length);
            return Stream.empty();
          }
          colConverters.forEach(c -> c.accept(sample, strings));
          return Stream.of(sample.toSample());
        };
      }

      private List<String> mustHaves =
          List.of(
              "completed_at_ms",
              "duration_ms",
              // Not required             "fail",
              "status",
              "bytes_up",
              // Not required             "bytes_down",
              // Not required             "call",
              // *should* be required, but will default to "": "label",
              "thread");

      @Override
      public boolean identifiedByHeaders(List<String> headers) {
        return headers.containsAll(mustHaves);
      }
    },
    JTL {
      @Override
      public Csvs.Deserializer<Sample> apply(List<String> headers) {
        int expectedColumns = headers.size();
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
                case "success" -> (sample, row) -> sample.setSuccess(parseBooleanStrict(row[col]));
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
          if (strings.length != expectedColumns) {
            LOG.warn(
                "Expected {} values, but got {} instead. Skipping row.",
                expectedColumns,
                strings.length);
            return Stream.empty();
          }
          try {
            colConverters.forEach(c -> c.accept(sample, strings));
            return Stream.of(sample);
          } catch (AppServerException | NumberFormatException ex) {
            LOG.debug("Skipping row, deserialization error", ex);
            return Stream.empty();
          }
        };
      }

      private static final Set<String> REQUIRED_COLUMNS =
          Set.of(
              "timeStamp",
              "elapsed",
              "label",
              "responseCode",
              "threadName",
              "success",
              "bytes",
              "allThreads");

      @Override
      public boolean identifiedByHeaders(List<String> headers) {
        if (headers.isEmpty()) {
          // No headers means that the file is empty, which this deserializer handles.
          return true;
        }
        return headers.containsAll(REQUIRED_COLUMNS);
      }
    }
  }

  private static boolean parseBooleanStrict(String s) {
    // This looks like it could be simplified, but we
    // need it to only deal with "tRuE" and "FaLsE" strings
    // (Boolean.parseBoolean(str) will treat "fAlSe", "banana", "yes", "no", "maybe", etc
    // all as false. We only want the "fAlSe" case to be treated as false, skip the rest)
    if (Boolean.parseBoolean(s)) {
      return true;
    } else if ("false".equalsIgnoreCase(s)) {
      return false;
    }
    throw new NumberFormatException("Not a boolean: " + s);
  }

  private interface IdentifierByHeader {
    /** True if the sourcetype can process CSVs with this set of headers. */
    boolean identifiedByHeaders(List<String> headers);
  }

  private static class LoadySample {
    long completedAtMs;
    long durationMs;
    int fail;
    String status;
    long bytesDown;
    String label = ""; // Default to blank if it isn't provided
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
