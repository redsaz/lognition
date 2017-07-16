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
import com.redsaz.meterrier.convert.AvroSamplesWriter;
import com.redsaz.meterrier.convert.CsvJtlSource;
import com.redsaz.meterrier.convert.Samples;
import com.redsaz.meterrier.convert.SamplesWriter;
import com.redsaz.meterrier.convert.model.PreSample;
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
 * Given PreSample data, creates statistics like: min, 25th %-ile, 50th %-ile, 75th %-ile, 90 %-ile,
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
        List<Stats> timeSeries = StatsBuilder.calcTimeSeriesStats(sourceSamples.getSamples(), 60000L);
        StatsBuilder.writeStatsCsv(timeSeries, statsFile);

        Map<String, List<PreSample>> labelsSamples = StatsBuilder.sortAndSplitByLabel(sourceSamples.getSamples());
        for (Map.Entry<String, List<PreSample>> entry : labelsSamples.entrySet()) {
            String label = entry.getKey();
            List<PreSample> labelSamples = entry.getValue();
            List<Stats> labelTimeSeries = StatsBuilder.calcTimeSeriesStats(labelSamples, 60000L);
            File labelDest = new File("../meterrier/jtls/target/converted/real-550cps-1hour-stats-60s-"
                    + sanitize(label) + ".csv");
            StatsBuilder.writeStatsCsv(labelTimeSeries, labelDest);
        }
    }

    private static String sanitize(String label) {
        return label.toLowerCase().replaceAll("[^0-9a-zA-Z]+", "-").replaceAll("(^-+)|(-+$)", "");
    }

//    public static Something calcStatsByLabel(List<PreSample> samples, long spanMillis) {
//        // First, sort the samples by label, then by offset.
//        Collections.sort(samples, LABEL_OFFSET_COMPARATOR);
//
//        // Now, for each label, get a sublist spanning the entire set of objects for that label.
//        Map<String, List<PreSample>> labelsLists = splitByLabel(samples);
//
//        // Within each sublist, find sublists (bins) for each segment of time, and calculate the
//        // stats for each.
//        Something labelsStats = new Something();
//        int numBins = (int) Math.ceil((double) spanMillis / 1000D);
//        for (Entry<String, List<PreSample>> labelList : labelsLists.entrySet()) {
//            StatsList statsList = createStatsList(labelList.getValue(), numBins, 1000L);
//            labelsStats.put(labelList.getKey(), statsList);
//        }
//        return labelsStats;
//    }
    /**
     * Calculates time series stats on a previously sorted (by offset) list of samples.
     *
     * @param offsetSortedSamples
     * @param spanMillis
     * @return
     */
    public static List<Stats> calcTimeSeriesStats(List<PreSample> offsetSortedSamples, long spanMillis) {
        // Find sublists (bins) for each segment of time, and calculate the
        // stats for each.
        double lastOffset = offsetSortedSamples.get(offsetSortedSamples.size() - 1).getOffset();
        int numBins = (int) Math.ceil((double) lastOffset / spanMillis);
        List<Stats> statsList = createStatsList(offsetSortedSamples, numBins, spanMillis);

        return statsList;
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
    public static Map<String, List<PreSample>> sortAndSplitByLabel(List<PreSample> samples) {
        Collections.sort(samples, LABEL_OFFSET_COMPARATOR);
        Map<String, List<PreSample>> labelLists = new TreeMap<>();
        int startIndex = 0;
        String currentLabel = samples.get(0).getLabel();
        for (int i = 0; i < samples.size(); ++i) {
            PreSample sample = samples.get(i);
            if (!currentLabel.equals(sample.getLabel())) {
                int endIndex = i;
                List<PreSample> samplesForLabel = samples.subList(startIndex, endIndex);
                labelLists.put(currentLabel, samplesForLabel);
                startIndex = i;
                currentLabel = sample.getLabel();
            }
        }
        // The last label needs included too.
        int endIndex = samples.size();
        List<PreSample> samplesForLabel = samples.subList(startIndex, endIndex);
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
    private static List<Stats> createStatsList(List<PreSample> samples, int numBins, long interval) {
        List<Stats> list = new ArrayList<>(numBins);
        int samplesToSkip = 0;
        for (int i = 0; i < numBins; ++i) {
            long endOffset = interval * (i + 1);
            // First find the samples for the timerange of the bin
            List<PreSample> binSamples = getSamplesWithinOffsets(samples, samplesToSkip, endOffset);
            samplesToSkip += binSamples.size();
            // Then sort those samples in order from shortest duration to longest so that we
            // can calculate the percentiles.
            Collections.sort(binSamples, DURATION_COMPARATOR);
            Stats binStats = new Stats(i * interval, binSamples);
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
    private static List<PreSample> getSamplesWithinOffsets(List<PreSample> samples, int numSkip, long maxOffset) {
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

    public static String writeStatsCsv(List<Stats> stats, File dest) {
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
                    writer.processRecords(stats);
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
                (System.currentTimeMillis() - startMillis), stats.size(), dest.getPath());
        return sha256Hash;
    }

    public static final Comparator<PreSample> TEMPORAL_COMPARATOR = (PreSample o1, PreSample o2) -> {
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
    public static final Comparator<PreSample> LABEL_OFFSET_COMPARATOR = (PreSample o1, PreSample o2) -> {
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
    public static final Comparator<PreSample> DURATION_COMPARATOR = (PreSample o1, PreSample o2) -> {
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
}
