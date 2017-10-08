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
package com.redsaz.meterrier.store;

import com.redsaz.meterrier.api.StatsService;
import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.redsaz.meterrier.api.model.Stats;
import com.redsaz.meterrier.api.model.Timeseries;
import static com.redsaz.meterrier.model.tables.Aggregate.AGGREGATE;
import static com.redsaz.meterrier.model.tables.SampleLabel.SAMPLE_LABEL;
import static com.redsaz.meterrier.model.tables.Timeseries.TIMESERIES;
import com.redsaz.meterrier.model.tables.records.AggregateRecord;
import com.redsaz.meterrier.model.tables.records.SampleLabelRecord;
import com.redsaz.meterrier.model.tables.records.TimeseriesRecord;
import com.univocity.parsers.common.Context;
import com.univocity.parsers.common.processor.BeanWriterProcessor;
import com.univocity.parsers.common.processor.core.Processor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep3;
import org.jooq.RecordMapper;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores and accesses statistical data.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class JooqStatsService implements StatsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqStatsService.class);

    private static final RecordToTimeseriesMapper R2TIMESERIES = new RecordToTimeseriesMapper();
    private static final RecordToSampleLabelMapper R2SAMPLE_LABEL = new RecordToSampleLabelMapper();
    private static final RecordToStatsMapper R2STATS = new RecordToStatsMapper();

    private final ConnectionPool pool;
    private final SQLDialect dialect;

    /**
     * Create a new LogsService backed by a data store.
     *
     * @param jdbcPool opens connections to database
     * @param sqlDialect the type of SQL database that we should speak
     * @param logsDirectory the directory containing the logs
     */
    public JooqStatsService(ConnectionPool jdbcPool, SQLDialect sqlDialect) {
        pool = jdbcPool;
        dialect = sqlDialect;
    }

    @Override
    public void createSampleLabels(long logId, List<String> labels) {
        if (labels == null) {
            throw new NullPointerException("No labels specified.");
        } else if (logId < 1L) {
            throw new IllegalArgumentException("Bad log id.");
        }

        LOGGER.info("Creating sample labels in DB for logId={}...", logId);
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, dialect);

            InsertValuesStep3<SampleLabelRecord, Long, Long, String> inserts = context.insertInto(
                    SAMPLE_LABEL,
                    SAMPLE_LABEL.LOG_ID,
                    SAMPLE_LABEL.LABEL_ID,
                    SAMPLE_LABEL.LABEL);
            for (int i = 0; i < labels.size(); ++i) {
                String label = labels.get(i);
                inserts = inserts.values(
                        logId,
                        Long.valueOf(i),
                        label);
            }
            inserts.execute();
            LOGGER.info("...Created sample labels in DB for logId={}.", logId);
        } catch (SQLException ex) {
            throw new AppServerException("Failed to create timeseries: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<String> getSampleLabels(long logId) {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, dialect);
            return context.selectFrom(SAMPLE_LABEL)
                    .where(SAMPLE_LABEL.LOG_ID.eq(logId))
                    .orderBy(SAMPLE_LABEL.LABEL_ID)
                    .fetch(SAMPLE_LABEL.LABEL);
        } catch (SQLException ex) {
            throw new AppServerException("Cannot get sample labels for logId=" + logId + " because: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Stats getAggregate(long logId, long labelId) {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, dialect);
            return context.selectFrom(AGGREGATE)
                    .where(AGGREGATE.LOG_ID.eq(logId))
                    .and(AGGREGATE.LABEL_ID.eq(labelId))
                    .fetchOne(R2STATS);
        } catch (SQLException ex) {
            throw new AppServerException("Cannot get aggregate for log=" + logId + " labelId="
                    + labelId + "because: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Timeseries getTimeseries(long logId, long labelId) {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, dialect);
            return context.selectFrom(TIMESERIES)
                    .where(TIMESERIES.LOG_ID.eq(logId))
                    .and(TIMESERIES.LABEL_ID.eq(labelId))
                    .and(TIMESERIES.SPAN_MILLIS.eq(60000L))
                    .fetchOne(R2TIMESERIES);
        } catch (SQLException ex) {
            throw new AppServerException("Cannot get timeseries_id=" + logId + " because: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void createOrUpdateAggregate(long logId, long labelId, Stats aggregate) {
        if (aggregate == null) {
            throw new NullPointerException("No aggregate was specified.");
        } else if (logId < 1L) {
            throw new IllegalArgumentException("Bad log id.");
        }

        LOGGER.info("Creating entry in DB...");
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, dialect);

            context.mergeInto(AGGREGATE,
                    AGGREGATE.LOG_ID,
                    AGGREGATE.LABEL_ID,
                    AGGREGATE.MIN,
                    AGGREGATE.P25,
                    AGGREGATE.P50,
                    AGGREGATE.P75,
                    AGGREGATE.P90,
                    AGGREGATE.P95,
                    AGGREGATE.P99,
                    AGGREGATE.MAX,
                    AGGREGATE.AVG,
                    AGGREGATE.NUM_SAMPLES,
                    AGGREGATE.TOTAL_RESPONSE_BYTES,
                    AGGREGATE.NUM_ERRORS
            ).values(
                    logId,
                    labelId,
                    aggregate.getMin(),
                    aggregate.getP25(),
                    aggregate.getP50(),
                    aggregate.getP75(),
                    aggregate.getP90(),
                    aggregate.getP95(),
                    aggregate.getP99(),
                    aggregate.getMax(),
                    aggregate.getAvg(),
                    aggregate.getNumSamples(),
                    aggregate.getTotalResponseBytes(),
                    aggregate.getNumErrors()
            ).execute();
            LOGGER.info("...Created aggregate entry in DB.");
        } catch (SQLException ex) {
            throw new AppServerException("Failed to create timeseries: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void createOrUpdateTimeseries(long logId, long labelId, Timeseries timeseries) {
        if (timeseries == null) {
            throw new NullPointerException("No timeseries was specified.");
        } else if (timeseries.getStatsList() == null) {
            throw new NullPointerException("No timeseries data was specified.");
        } else if (timeseries.getSpanMillis() < 1L) {
            throw new IllegalArgumentException("Bad resolution (ms) for timeseries.");
        } else if (logId < 1L) {
            throw new IllegalArgumentException("Bad log id.");
        }

        byte[] compressedStatsBytes = convertToSeriesData(timeseries);

        LOGGER.info("Creating entry in DB...");
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, dialect);

            context.mergeInto(TIMESERIES,
                    TIMESERIES.LOG_ID,
                    TIMESERIES.LABEL_ID,
                    TIMESERIES.SPAN_MILLIS,
                    TIMESERIES.SERIES_DATA).values(
                            logId,
                            labelId,
                            timeseries.getSpanMillis(),
                            compressedStatsBytes)
                    .execute();
            LOGGER.info("...Created timeseries entry in DB.");
        } catch (SQLException ex) {
            throw new AppServerException("Failed to create timeseries: " + ex.getMessage(), ex);
        }
    }

    private static byte[] convertToSeriesData(Timeseries timeseries) {
        return writeTimeseriesCsv(timeseries.getStatsList());
    }

    private static Timeseries convertToTimeseries(long resolutionMillis, byte[] seriesData) {
        List<Stats> series = readTimeseriesCsv(seriesData);
        return new Timeseries(resolutionMillis, series);
    }

    public static byte[] writeTimeseriesCsv(List<Stats> stats) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Writer w = new OutputStreamWriter(baos)) {
            CsvWriter writer = null;
            try {
                CsvWriterSettings settings = new CsvWriterSettings();
                settings.setRowWriterProcessor(new BeanWriterProcessor<>(Stats.class));
                settings.setHeaders(Stats.HEADERS);
                writer = new CsvWriter(w, settings);

                writer.writeHeaders();
                writer.processRecords(stats);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not write stats data.", ex);
        }
        return baos.toByteArray();
    }

    public static List<Stats> readTimeseriesCsv(byte[] seriesData) {
        List<Stats> series = new ArrayList<>();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(seriesData)) {
            CsvParserSettings settings = new CsvParserSettings();
            settings.setHeaderExtractionEnabled(true);
            settings.setProcessor(new Processor<Context>() {
                @Override
                public void processStarted(Context context) {
                    // Do nothing.
                }

                @Override
                public void rowProcessed(String[] row, Context context) {
                    long offsetMillis = Long.parseLong(row[0]);
                    Long min = getLongOrNull(row[1]);
                    Long p25 = getLongOrNull(row[2]);
                    Long p50 = getLongOrNull(row[3]);
                    Long p75 = getLongOrNull(row[4]);
                    Long p90 = getLongOrNull(row[5]);
                    Long p95 = getLongOrNull(row[6]);
                    Long p99 = getLongOrNull(row[7]);
                    Long max = getLongOrNull(row[8]);
                    Long avg = getLongOrNull(row[9]);
                    long numSamples = Long.parseLong(row[10]);
                    long totalResponseBytes = Long.parseLong(row[11]);
                    long numErrors = Long.parseLong(row[12]);
                    Stats stats = new Stats(offsetMillis, min, p25, p50, p75, p90, p95, p99, max, avg,
                            numSamples, totalResponseBytes, numErrors);
                    series.add(stats);
                }

                @Override
                public void processEnded(Context context) {
                    // Do nothing.
                }

                private Long getLongOrNull(String val) {
                    if ("".equals(val) || "null".equals(val) || val == null) {
                        return null;
                    }
                    return Long.valueOf(val);
                }

            });
            CsvParser parser = new CsvParser(settings);
            parser.parse(bais, Charset.forName("UTF8"));
        } catch (IOException ex) {
            throw new RuntimeException("Could not write stats data.", ex);
        }
        return series;
    }

    private static class RecordToTimeseriesMapper implements RecordMapper<TimeseriesRecord, Timeseries> {

        @Override
        public Timeseries map(TimeseriesRecord record) {
            if (record == null) {
                return null;
            }
            return convertToTimeseries(
                    record.getSpanMillis(),
                    record.getSeriesData()
            );
        }
    }

    private static class RecordToSampleLabelMapper implements RecordMapper<SampleLabelRecord, String> {

        @Override
        public String map(SampleLabelRecord record) {
            if (record == null) {
                return null;
            }
            return record.getLabel();
        }
    }

    private static class RecordToStatsMapper implements RecordMapper<AggregateRecord, Stats> {

        @Override
        public Stats map(AggregateRecord record) {
            if (record == null) {
                return null;
            }

            return new Stats(0L, record.getMin(), record.getP25(), record.getP50(), record.getP75(),
                    record.getP90(), record.getP95(), record.getP99(), record.getMax(),
                    record.getAvg(), record.getNumSamples(), record.getTotalResponseBytes(),
                    record.getNumErrors());
        }
    }

}
