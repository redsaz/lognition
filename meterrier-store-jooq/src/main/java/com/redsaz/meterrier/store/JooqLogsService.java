/*
 * Copyright 2016 Redsaz <redsaz@gmail.com>.
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

import com.redsaz.meterrier.api.LogsService;
import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.redsaz.meterrier.api.model.Log;
import com.redsaz.meterrier.api.model.Stats;
import static com.redsaz.meterrier.model.tables.Log.LOG;
import com.redsaz.meterrier.model.tables.records.LogRecord;
import com.univocity.parsers.common.Context;
import com.univocity.parsers.common.processor.core.Processor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.RecordHandler;
import org.jooq.RecordMapper;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores and accesses logs.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class JooqLogsService implements LogsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqLogsService.class);

    private static final RecordToLogMapper R2L = new RecordToLogMapper();

    private final ConnectionPool pool;
    private final SQLDialect dialect;
    private final String logsDir;

    /**
     * Create a new LogsService backed by a data store.
     *
     * @param jdbcPool opens connections to database
     * @param sqlDialect the type of SQL database that we should speak
     * @param logsDirectory the directory containing the logs
     */
    public JooqLogsService(ConnectionPool jdbcPool, SQLDialect sqlDialect, String logsDirectory) {
        pool = jdbcPool;
        dialect = sqlDialect;
        logsDir = logsDirectory;
    }

    @Override
    public Log create(Log source) {
        if (source == null) {
            throw new NullPointerException("No log information was specified.");
        } else if (source.getNotes() == null) {
            throw new NullPointerException("Log notes must not be null.");
        } else if (source.getTitle() == null) {
            throw new NullPointerException("Log title must not be null.");
        } else if (source.getUriName() == null) {
            throw new NullPointerException("Log uriName must not be null.");
        }

        LOGGER.info("Creating entry in DB...");
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, dialect);

            LogRecord result = context.insertInto(LOG,
                    LOG.URINAME,
                    LOG.TITLE,
                    LOG.UPLOADEDUTCMILLIS,
                    LOG.DATAFILE,
                    LOG.NOTES).values(
                            source.getUriName(),
                            source.getTitle(),
                            source.getUploadedUtcMillis(),
                            source.getDataFile(),
                            source.getNotes())
                    .returning().fetchOne();
            LOGGER.info("...Created log entry in DB.");
            return R2L.map(result);
        } catch (SQLException ex) {
            throw new AppServerException("Failed to create log: " + ex.getMessage(), ex);
        }

    }

    @Override
    public OutputStream getContent(long id) {
//        try (Connection c = POOL.getConnection()) {
//            DSLContext context = DSL.using(c, dialect);
//
//            LogRecord nr = context.selectFrom(LOG).where(LOG.ID.eq(id)).fetchOne();
//            return recordToLog(nr);
//        } catch (SQLException ex) {
//            throw new AppServerException("Cannot get log_id=" + id + " because: " + ex.getMessage(), ex);
//        }
        return null;
    }

    @Override
    public List<Stats> getOverallTimeseries(long id) {
        List<Stats> series = new ArrayList<>();
        File overallFile = new File(logsDir, id + "-overall-timeseries-60s.csv");
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
                Long min = Long.valueOf(row[1]);
                Long p25 = Long.valueOf(row[2]);
                Long p50 = Long.valueOf(row[3]);
                Long p75 = Long.valueOf(row[4]);
                Long p90 = Long.valueOf(row[5]);
                Long p95 = Long.valueOf(row[6]);
                Long p99 = Long.valueOf(row[7]);
                Long max = Long.valueOf(row[8]);
                Long avg = Long.valueOf(row[9]);
                long numSamples = Long.valueOf(row[10]);
                long totalResponseBytes = Long.valueOf(row[11]);
                long numErrors = Long.valueOf(row[12]);
                Stats stats = new Stats(offsetMillis, min, p25, p50, p75, p90, p95, p99, max, avg,
                        numSamples, totalResponseBytes, numErrors);
                series.add(stats);
            }

            @Override
            public void processEnded(Context context) {
                // Do nothing.
            }

        });
        CsvParser parser = new CsvParser(settings);
        parser.parse(overallFile, Charset.forName("UTF8"));
        return series;
    }

    @Override
    public Log get(long id) {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, dialect);
            return context.selectFrom(LOG)
                    .where(LOG.ID.eq(id))
                    .fetchOne(R2L);
        } catch (SQLException ex) {
            throw new AppServerException("Cannot get log_id=" + id + " because: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Log> list() {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, dialect);
            RecordsToListHandler r2lHandler = new RecordsToListHandler();
            return context.selectFrom(LOG).fetchInto(r2lHandler).getLogs();
        } catch (SQLException ex) {
            throw new AppServerException("Cannot get logs list");
        }
    }

    @Override
    public void delete(long id) {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, dialect);

            context.delete(LOG).where(LOG.ID.eq(id)).execute();
        } catch (SQLException ex) {
            throw new AppServerException("Failed to delete log_id=" + id
                    + " because: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Log update(Log source) {
        if (source == null) {
            throw new NullPointerException("No log information was specified.");
        } else if (source.getNotes() == null) {
            throw new NullPointerException("Log notes must not be null.");
        } else if (source.getTitle() == null) {
            throw new NullPointerException("Log title must not be null.");
        } else if (source.getUriName() == null) {
            throw new NullPointerException("Log uriName must not be null.");
        }

        LOGGER.info("Updating entry in DB...");
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, dialect);

            LogRecord result = context.update(LOG)
                    .set(LOG.URINAME, source.getUriName())
                    .set(LOG.TITLE, source.getTitle())
                    .set(LOG.UPLOADEDUTCMILLIS, source.getUploadedUtcMillis())
                    .set(LOG.NOTES, source.getNotes())
                    .where(LOG.ID.eq(source.getId()))
                    .returning().fetchOne();
            LOGGER.info("...Updated entry in DB.");
            return R2L.map(result);
        } catch (SQLException ex) {
            throw new AppServerException("Failed to update log: " + ex.getMessage(), ex);
        }
    }

    private static class RecordToLogMapper implements RecordMapper<LogRecord, Log> {

        @Override
        public Log map(LogRecord record) {
            if (record == null) {
                return null;
            }
            return new Log(record.getId(),
                    record.getUriname(),
                    record.getTitle(),
                    record.getUploadedutcmillis(),
                    record.getDatafile(),
                    record.getNotes()
            );
        }
    }

    private static class RecordsToListHandler implements RecordHandler<LogRecord> {

        private final List<Log> logs = new ArrayList<>();

        @Override
        public void next(LogRecord record) {
            logs.add(R2L.map(record));
        }

        public List<Log> getLogs() {
            return logs;
        }
    }

}
