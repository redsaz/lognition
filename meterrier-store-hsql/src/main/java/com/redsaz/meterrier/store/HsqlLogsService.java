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

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.hsqldb.jdbc.JDBCPool;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import com.redsaz.meterrier.api.LogsService;
import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.redsaz.meterrier.api.model.Log;
import static com.redsaz.meterrier.model.tables.Log.LOG;
import com.redsaz.meterrier.model.tables.records.LogRecord;
import java.util.ArrayList;
import org.jooq.RecordHandler;
import org.jooq.RecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores and accesses logs.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class HsqlLogsService implements LogsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HsqlLogsService.class);

    private static final RecordToLogMapper R2L = new RecordToLogMapper();

    private final JDBCPool pool;

    /**
     * Create a new HSQLDB-base LogsService.
     *
     * @param jdbcPool opens connections to database
     */
    public HsqlLogsService(JDBCPool jdbcPool) {
        LOGGER.info("Using given JDBC Pool.");
        pool = jdbcPool;
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
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

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
//            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
//
//            LogRecord nr = context.selectFrom(LOG).where(LOG.ID.eq(id)).fetchOne();
//            return recordToLog(nr);
//        } catch (SQLException ex) {
//            throw new AppServerException("Cannot get log_id=" + id + " because: " + ex.getMessage(), ex);
//        }
        return null;
    }

    @Override
    public Log get(long id) {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
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
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
            RecordsToListHandler r2lHandler = new RecordsToListHandler();
            return context.selectFrom(LOG).fetchInto(r2lHandler).getLogs();
        } catch (SQLException ex) {
            throw new AppServerException("Cannot get logs list");
        }
    }

    @Override
    public void delete(long id) {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

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
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

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
