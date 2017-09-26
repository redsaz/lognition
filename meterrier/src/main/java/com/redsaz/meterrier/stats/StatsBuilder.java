/*
 * Copyright 2017 Redsaz <redsaz@gmail.com>.
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
package com.redsaz.meterrier.stats;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.redsaz.meterrier.api.model.Sample;
import com.redsaz.meterrier.api.model.Stats;
import com.redsaz.meterrier.api.model.Timeseries;
import com.redsaz.meterrier.convert.AvroSamplesWriter;
import com.redsaz.meterrier.convert.CsvJtlSource;
import com.redsaz.meterrier.convert.Samples;
import com.redsaz.meterrier.convert.SamplesWriter;
import com.univocity.parsers.common.processor.BeanWriterProcessor;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given sample data, creates statistics like: min, 25th %-ile, 50th %-ile, 75th %-ile, 90 %-ile,
 * 95th %-ile, 99th %-ile, max, average, #-of-calls, total-bytes, #-of-errors
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class StatsBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatsBuilder.class);

    public static void main(String[] args) throws IOException {
//        File source = new File("../meterrier/jtls/target/real-large.jtl");
//        File dest = new File("../meterrier/jtls/target/converted/real-large.avro");

//        File source = new File("../meterrier/jtls/target/real-without-header.jtl");
//        File dest = new File("../meterrier/jtls/target/converted/real-without-header.avro");
        File source = new File("../meterrier/jtls/target/real-550cps-1hour.jtl");
        File dest = new File("../meterrier/jtls/target/converted/real-550cps-1hour.avro");
        File statsFile = new File("../meterrier/jtls/target/converted/real-550cps-1hour-stats-60s.csv");

        Samples sourceSamples = new CsvJtlSource(source);
        SamplesWriter writer = new AvroSamplesWriter();
        writer.write(sourceSamples, dest);
        Timeseries timeseries = StatsBuilder.calcTimeSeriesStats(sourceSamples.getSamples(), 60000L);
        StatsBuilder.writeStatsCsv(timeseries, statsFile);

        Map<String, List<Sample>> labelsSamples = StatsBuilder.sortAndSplitByLabel(sourceSamples.getSamples());
        for (Map.Entry<String, List<Sample>> entry : labelsSamples.entrySet()) {
            String label = entry.getKey();
            List<Sample> labelSamples = entry.getValue();
            Timeseries labelTimeseries = StatsBuilder.calcTimeSeriesStats(labelSamples, 60000L);
            File labelDest = new File("../meterrier/jtls/target/converted/real-550cps-1hour-stats-60s-"
                    + sanitize(label) + ".csv");
            StatsBuilder.writeStatsCsv(labelTimeseries, labelDest);
        }
    }

    private static String sanitize(String label) {
        return label.toLowerCase().replaceAll("[^0-9a-zA-Z]+", "-").replaceAll("(^-+)|(-+$)", "");
    }

    /**
     * Calculates time series stats on a previously sorted (by offset) list of samples.
     *
     * @param offsetSortedSamples
     * @param spanMillis
     * @return
     */
    public static Timeseries calcTimeSeriesStats(List<Sample> offsetSortedSamples, long spanMillis) {
        // Find sublists (bins) for each segment of time, and calculate the
        // stats for each.
        double lastOffset = offsetSortedSamples.get(offsetSortedSamples.size() - 1).getOffset();
        int numBins = (int) Math.ceil((double) lastOffset / spanMillis);
        List<Stats> statsList = createStatsList(offsetSortedSamples, numBins, spanMillis);

        return new Timeseries(spanMillis, statsList);
    }

    /**
     * Given a list of samples, this will first sort the list by label and then by offset. What is
     * returned is a map of key=label value=sublist, where each sublist are the samples related to
     * that label.
     * <p>
     * Once the sublists are no longer needed, you may sort the master samples list again how you
     * see fit.
     *
     * @param samples The samples to sort and return values.
     * @return a map where key=label, value=sublist where all elements are for that label.
     */
    public static Map<String, List<Sample>> sortAndSplitByLabel(List<Sample> samples) {
        Collections.sort(samples, LABEL_OFFSET_COMPARATOR);
        Map<String, List<Sample>> labelLists = new TreeMap<>();
        int startIndex = 0;
        String currentLabel = samples.get(0).getLabel();
        for (int i = 0; i < samples.size(); ++i) {
            Sample sample = samples.get(i);
            if (!currentLabel.equals(sample.getLabel())) {
                int endIndex = i;
                List<Sample> samplesForLabel = samples.subList(startIndex, endIndex);
                labelLists.put(currentLabel, samplesForLabel);
                startIndex = i;
                currentLabel = sample.getLabel();
            }
        }
        // The last label needs included too.
        int endIndex = samples.size();
        List<Sample> samplesForLabel = samples.subList(startIndex, endIndex);
        labelLists.put(currentLabel, samplesForLabel);

        return labelLists;
    }

    /**
     * Creates a list of stats of samples over time, in bins the size of the interval. So, say that
     * the interval is 1000 milliseconds, and there are 3600 bins. This means that the first bin
     * will be in the range 0ms (inclusive) to 1000ms (exlusive), the second bin will have the range
     * 1000ms (inclusive) to 2000ms (exclusive) and so on until the last bin of 3599000ms
     * (inclusive) to 3600000ms (exclusive). If no samples are available for a particular bin, then
     * the bin will be null.
     * <p>
     * NOTE: The samples will need to be sorted by offset (earliest to latest) for this method to
     * work right. work.
     * <p>
     * WARNING: This alters the order of the samples. The samples will need to be resorted by offset
     * if further stats will be taken.
     *
     * @param samples data which is already-ordered-by-earliest-to-latest-offset
     * @param numBins How many elements the resulting list will have
     * @param interval The size (in millis) of each bin
     * @return a list of stats
     */
    private static List<Stats> createStatsList(List<Sample> samples, int numBins, long interval) {
        List<Stats> list = new ArrayList<>(numBins);
        int samplesToSkip = 0;
        for (int i = 0; i < numBins; ++i) {
            long endOffset = interval * (i + 1);
            // First find the samples for the timerange of the bin
            List<Sample> binSamples = getSamplesWithinOffsets(samples, samplesToSkip, endOffset);
            samplesToSkip += binSamples.size();
            // Then sort those samples in order from shortest duration to longest so that we
            // can calculate the percentiles.
            Collections.sort(binSamples, DURATION_COMPARATOR);
            Stats binStats = createStats(i * interval, binSamples);
            list.add(i, binStats);
        }
        return list;
    }

    /**
     * Gets a sublist of samples from the list, skipping the first number of elements, which are
     * before the endOffset. Actually, it will find a sublist starting at numSkip, and will stop one
     * element before the element that is equal or greater than the endOffset, so for it to work
     * properly, the original samples list must be sorted earliest-to-latest, (at least starting at
     * numSkip).
     * <p>
     * If there are no samples that are before endOffset, then an empty list is returned.
     *
     * @param samples The list to find a sublist within
     * @param numSkip The number of elements to skip
     * @param maxOffset The elements in the resulting sublist will have an offset earlier than this
     * @return a sublist of samples before endOffset, or an empty list if no samples are before the
     * offset.
     */
    private static List<Sample> getSamplesWithinOffsets(List<Sample> samples, int numSkip, long maxOffset) {
        if (samples.size() <= numSkip) {
            return Collections.emptyList();
        }
        for (int i = numSkip; i < samples.size(); ++i) {
            if (samples.get(i).getOffset() >= maxOffset) {
                return samples.subList(numSkip, i);
            }
        }
        return samples.subList(numSkip, samples.size());
    }

    public static String writeStatsCsv(Timeseries timeseries, File dest) {
        long startMillis = System.currentTimeMillis();
        String sha256Hash = null;
        try (HashingOutputStream hos = new HashingOutputStream(Hashing.sha256(), new BufferedOutputStream(new FileOutputStream(dest)))) {
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(hos, "UTF-8"))) {
                CsvWriter writer = null;
                try {
                    CsvWriterSettings settings = new CsvWriterSettings();
                    settings.setRowWriterProcessor(new BeanWriterProcessor<>(Stats.class));
                    settings.setHeaders(Stats.HEADERS);
                    writer = new CsvWriter(bw, settings);

                    writer.writeHeaders();
                    writer.processRecords(timeseries.getStatsList());
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
            }
            sha256Hash = hos.hash().toString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not write stats file " + dest.toString() + ".", ex);
        }
        LOGGER.info("Took {}ms to write {} lines of CSV for {}.",
                (System.currentTimeMillis() - startMillis), timeseries.getStatsList().size(), dest.getPath());
        return sha256Hash;
    }

    public static final Comparator<Sample> TEMPORAL_COMPARATOR = (Sample o1, Sample o2) -> {
        if (o1 == o2) {
            return 0;
        } else if (o2 == null) {
            return 1;
        } else if (o1 == null) {
            return -1;
        } else if (o1.getOffset() < o2.getOffset()) {
            return -1;
        } else if (o1.getOffset() > o2.getOffset()) {
            return 1;
        } else if (o1.getDuration() < o2.getDuration()) {
            return -1;
        } else if (o1.getDuration() > o2.getDuration()) {
            return 1;
        } else if (o1.getLabel() == null && o2.getLabel() != null) {
            return -1;
        } else if (o1.getLabel() != null && o2.getLabel() == null) {
            return 1;
        }
        int comp = o1.getLabel().compareTo(o2.getLabel());
        if (comp != 0) {
            return comp;
        }
        if (o1.getThreadName() == null && o2.getThreadName() != null) {
            return -1;
        } else if (o1.getThreadName() != null && o2.getThreadName() == null) {
            return 1;
        }
        comp = o1.getThreadName().compareTo(o2.getThreadName());
        if (comp != 0) {
            return comp;
        }
        if (o1.getResponseBytes() < o2.getResponseBytes()) {
            return -1;
        } else if (o1.getResponseBytes() > o2.getResponseBytes()) {
            return 1;
        }
        comp = o1.getStatusCode().compareTo(o2.getStatusCode());
        if (comp != 0) {
            return comp;
        }
        comp = o1.getStatusMessage().compareTo(o2.getStatusMessage());
        if (comp != 0) {
            return comp;
        }
        if (o1.isSuccess() != o2.isSuccess()) {
            if (o1.isSuccess()) {
                return -1;
            } else {
                return 1;
            }
        }
        if (o1.getTotalThreads() < o2.getTotalThreads()) {
            return -1;
        } else if (o1.getTotalThreads() > o2.getTotalThreads()) {
            return 1;
        }
        return 0;
    };

    /**
     * Sorts by label, then by offset. Everything else is sorted as normal.
     */
    public static final Comparator<Sample> LABEL_OFFSET_COMPARATOR = (Sample o1, Sample o2) -> {
        if (o1 == o2) {
            return 0;
        } else if (o2 == null) {
            return 1;
        } else if (o1 == null) {
            return -1;
        } else if (o1.getLabel() == null && o2.getLabel() != null) {
            return -1;
        } else if (o1.getLabel() != null && o2.getLabel() == null) {
            return 1;
        }
        int comp = o1.getLabel().compareTo(o2.getLabel());
        if (comp != 0) {
            return comp;
        }
        if (o1.getOffset() < o2.getOffset()) {
            return -1;
        } else if (o1.getOffset() > o2.getOffset()) {
            return 1;
        } else if (o1.getDuration() < o2.getDuration()) {
            return -1;
        } else if (o1.getDuration() > o2.getDuration()) {
            return 1;
        } else if (o1.getThreadName() == null && o2.getThreadName() != null) {
            return -1;
        } else if (o1.getThreadName() != null && o2.getThreadName() == null) {
            return 1;
        }
        comp = o1.getThreadName().compareTo(o2.getThreadName());
        if (comp != 0) {
            return comp;
        }
        if (o1.getResponseBytes() < o2.getResponseBytes()) {
            return -1;
        } else if (o1.getResponseBytes() > o2.getResponseBytes()) {
            return 1;
        }
        comp = o1.getStatusCode().compareTo(o2.getStatusCode());
        if (comp != 0) {
            return comp;
        }
        comp = o1.getStatusMessage().compareTo(o2.getStatusMessage());
        if (comp != 0) {
            return comp;
        }
        if (o1.isSuccess() != o2.isSuccess()) {
            if (o1.isSuccess()) {
                return -1;
            } else {
                return 1;
            }
        }
        if (o1.getTotalThreads() < o2.getTotalThreads()) {
            return -1;
        } else if (o1.getTotalThreads() > o2.getTotalThreads()) {
            return 1;
        }
        return 0;
    };

    /**
     * Sorts by by duration. Everything else is sorted as normal.
     */
    public static final Comparator<Sample> DURATION_COMPARATOR = (Sample o1, Sample o2) -> {
        if (o1 == o2) {
            return 0;
        } else if (o2 == null) {
            return 1;
        } else if (o1 == null) {
            return -1;
        } else if (o1.getDuration() < o2.getDuration()) {
            return -1;
        } else if (o1.getDuration() > o2.getDuration()) {
            return 1;
        } else if (o1.getOffset() < o2.getOffset()) {
            return -1;
        } else if (o1.getOffset() > o2.getOffset()) {
            return 1;
        } else if (o1.getLabel() == null && o2.getLabel() != null) {
            return -1;
        } else if (o1.getLabel() != null && o2.getLabel() == null) {
            return 1;
        }
        int comp = o1.getLabel().compareTo(o2.getLabel());
        if (comp != 0) {
            return comp;
        }
        if (o1.getThreadName() == null && o2.getThreadName() != null) {
            return -1;
        } else if (o1.getThreadName() != null && o2.getThreadName() == null) {
            return 1;
        }
        comp = o1.getThreadName().compareTo(o2.getThreadName());
        if (comp != 0) {
            return comp;
        }
        if (o1.getResponseBytes() < o2.getResponseBytes()) {
            return -1;
        } else if (o1.getResponseBytes() > o2.getResponseBytes()) {
            return 1;
        }
        comp = o1.getStatusCode().compareTo(o2.getStatusCode());
        if (comp != 0) {
            return comp;
        }
        comp = o1.getStatusMessage().compareTo(o2.getStatusMessage());
        if (comp != 0) {
            return comp;
        }
        if (o1.isSuccess() != o2.isSuccess()) {
            if (o1.isSuccess()) {
                return -1;
            } else {
                return 1;
            }
        }
        if (o1.getTotalThreads() < o2.getTotalThreads()) {
            return -1;
        } else if (o1.getTotalThreads() > o2.getTotalThreads()) {
            return 1;
        }
        return 0;
    };

    /**
     * Creates stats based on the provided samples which should already be ordered from shortest
     * duration to longest.
     *
     * @param offsetMillis The point in time, with 0 being the start of the test, that these stats
     * start at
     * @param durationOrderedSamples The samples to calculate the stats on.
     */
    private static Stats createStats(long offsetMillis, List<Sample> durationOrderedSamples) {
        Long min = null;
        Long p25 = null;
        Long p50 = null;
        Long p75 = null;
        Long p90 = null;
        Long p95 = null;
        Long p99 = null;
        Long max = null;

        if (!durationOrderedSamples.isEmpty()) {
            min = durationOrderedSamples.get(0).getDuration();
            p25 = getElement(durationOrderedSamples, 0.25D).getDuration();
            p50 = getElement(durationOrderedSamples, 0.50D).getDuration();
            p75 = getElement(durationOrderedSamples, 0.75D).getDuration();
            p90 = getElement(durationOrderedSamples, 0.90D).getDuration();
            p95 = getElement(durationOrderedSamples, 0.95D).getDuration();
            p99 = getElement(durationOrderedSamples, 0.99D).getDuration();
            max = durationOrderedSamples.get(durationOrderedSamples.size() - 1).getDuration();
        }
        long numSamples = durationOrderedSamples.size();
        long cumulativeDuration = 0;
        long cumulativeResponseBytes = 0;
        long cumulativeErrors = 0;
        for (int i = 0; i < durationOrderedSamples.size(); ++i) {
            Sample sample = durationOrderedSamples.get(i);
            cumulativeDuration += sample.getDuration();
            cumulativeResponseBytes += sample.getResponseBytes();
            if (!sample.isSuccess()) {
                ++cumulativeErrors;
            }
        }
        Long avg = null;
        if (numSamples != 0) {
            avg = cumulativeDuration / numSamples;
        }
        return new Stats(offsetMillis, min, p25, p50, p75, p90, p95, p99, max, avg, numSamples, cumulativeResponseBytes, cumulativeErrors);
    }

    private static <T> T getElement(List<T> items, double percent) {
        int index = (int) Math.ceil(((double) (items.size() - 1)) * percent);
        return items.get(index);
    }

}
