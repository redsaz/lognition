package com.redsaz.lognition.convert;

import com.redsaz.lognition.api.model.Sample;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * @param samples The samples themselves
 * @param labels The labels mentioned in samples
 * @param statusCodeLookup The status codes mentioned in samples
 * @param threadNames The thread names mentioned in samples
 * @param startTimestamp The earliest possible absolute (not relative) time across all samples. It
 *     is a lower bound to all the samples. This is when the earliest-starting sample started.
 * @param finishTimestamp The latest possible absolute (not relative) time across all samples. It is
 *     an upper bound to all the samples. This is when the latest-ending sample ended.
 */
public record ListSamples(
    // TODO: Is unfortunately an array list because it gets modified/sorted later. Boo!
    ArrayList<Sample> samples,
    List<String> labels,
    StatusCodeLookup statusCodeLookup,
    List<String> threadNames,
    Instant startTimestamp,
    Instant finishTimestamp)
    implements Samples {

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public long getEarliestMillis() {
    if (samples.isEmpty()) {
      return 0L;
    }
    return startTimestamp().toEpochMilli();
  }

  @Override
  public Sample getEarliestSample() {
    if (samples.isEmpty()) {
      return null;
    }
    return samples().getFirst();
  }

  @Override
  public List<String> getLabels() {
    return labels();
  }

  @Override
  public long getLatestMillis() {
    if (samples.isEmpty()) {
      return 0L;
    }
    return finishTimestamp().toEpochMilli();
  }

  @Override
  public Sample getLatestSample() {
    if (samples.isEmpty()) {
      return null;
    }
    return samples().getLast();
  }

  @Override
  public List<Sample> getSamples() {
    return samples();
  }

  @Override
  public StatusCodeLookup getStatusCodeLookup() {
    return statusCodeLookup();
  }

  @Override
  public List<String> getThreadNames() {
    return threadNames();
  }

  public static class Builder {
    private final ArrayList<Sample> samples = new ArrayList<>();
    private final SortedSet<String> labels = new TreeSet<>();
    private final SortedSet<String> threadNames = new TreeSet<>();
    private final StatusCodeLookup statusCodeLookup = new StatusCodeLookup();
    private Instant earliestStart;
    private Instant latestFinish;
    private final Map<String, String> stringPool = new HashMap<>();

    private Builder() {}

    public ListSamples build() {
      Collections.sort(samples);
      return new ListSamples(
          samples,
          List.copyOf(labels),
          statusCodeLookup,
          List.copyOf(threadNames),
          earliestStart,
          latestFinish);
    }

    public Builder add(Sample sample) {
      recalcLatest(sample);
      sample.setOffset(sample.getOffset() - recalc0Offset(sample.getOffset()));
      stringPoolerize(sample);
      if (sample.getLabel() != null) {
        labels.add(sample.getLabel());
      }
      if (sample.getThreadName() != null) {
        threadNames.add(sample.getThreadName());
      }
      statusCodeLookup.getRef(sample.getStatusCode(), sample.getStatusMessage());
      samples.add(sample);

      return this;
    }

    // The earliest sample should have an offset of 0. As the timestamps arrive, they are altered to
    // be relative to the 0 offset. If one comes in earlier than the earliest known timestamp, all
    // samples need recalculated to the new offset.
    private long recalc0Offset(long newOriginalOffset) {
      Instant timestamp = Instant.ofEpochMilli(newOriginalOffset);
      if (earliestStart == null) {
        // The new offset is the first/only offset. Use it, and there's nothing to adjust.
        earliestStart = timestamp;
        return newOriginalOffset;
      }
      if (timestamp.isAfter(earliestStart)) {
        return earliestStart.toEpochMilli();
      }

      // the new offset is earlier than the current earliest. Everything done already needs to move
      // "later" to account for it. This assumes that we are fed samples in mostly start to finish
      // order, otherwise this is slow.
      long adjustment = earliestStart.toEpochMilli() - newOriginalOffset;
      samples.forEach(sample -> sample.setOffset(sample.getOffset() + adjustment));
      earliestStart = timestamp;
      return newOriginalOffset;
    }

    private void recalcLatest(Sample sample) {
      Instant finishTimestamp = Instant.ofEpochMilli(sample.getOffset() + sample.getDuration());
      if (latestFinish == null || latestFinish.isBefore(finishTimestamp)) {
        latestFinish = finishTimestamp;
      }
    }

    // Like String.intern(), but not global
    private void stringPoolerize(Sample sample) {
      sample.setLabel(stringPoolerize(sample.getLabel()));
      sample.setStatusMessage(stringPoolerize(sample.getStatusMessage()));
      sample.setStatusCode(stringPoolerize(sample.getStatusCode()));
      sample.setThreadName(stringPoolerize(sample.getThreadName()));
    }

    private String stringPoolerize(String s) {
      if (s == null) {
        return s;
      }
      return stringPool.computeIfAbsent(s, Function.identity());
    }
  }
}
