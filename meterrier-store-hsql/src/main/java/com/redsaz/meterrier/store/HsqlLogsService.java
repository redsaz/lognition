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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.hsqldb.jdbc.JDBCPool;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import static com.redsaz.meterrier.model.tables.Log.LOG;
import com.redsaz.meterrier.api.LogsService;
import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.redsaz.meterrier.api.model.Log;
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
        File originalLogsDir = new File("./meterrier-data/original-logs");
        try {
            Files.createDirectories(originalLogsDir.toPath());
        } catch (IOException ex) {
            throw new RuntimeException("Unable to create data directories.", ex);
        }
    }

    @Override
    public Log createLog(InputStream raw, Log source) {
        if (raw == null) {
            throw new NullPointerException("No log was specified.");
        } else if (source == null) {
            throw new NullPointerException("No log information was specified.");
        } else if (source.getNotes() == null) {
            throw new NullPointerException("Log notes must not be null.");
        } else if (source.getTitle() == null) {
            throw new NullPointerException("Log title must not be null.");
        } else if (source.getUriName() == null) {
            throw new NullPointerException("Log uriName must not be null.");
        }

        LOGGER.info("Storing log file...");
        File firstFile = getTempFile();
        LOGGER.info("Storing initially into {}", firstFile.getAbsolutePath());
        File digestFile;
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(firstFile))) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buff = new byte[4096];
            int num;
            while ((num = raw.read(buff)) >= 0) {
                md.update(buff, 0, num);
                os.write(buff, 0, num);
            }
            os.flush();
            String digestHex = bytesToHex(md.digest());
            digestFile = getDigestFile(digestHex);
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new AppServerException("Failed to upload content.", ex);
        }
        if (!Files.exists(digestFile.toPath())) {
            try {
                LOGGER.info("Moving content to permanent home {}...", digestFile);
                Files.move(firstFile.toPath(), digestFile.toPath());
                LOGGER.info("Moved  content to permanent home {}...", digestFile);
            } catch (IOException ex) {
                firstFile.delete(); // Assume the move failed so delete original.
                throw new AppServerException("Failed to upload content.", ex);
            }
        } else {
            LOGGER.info("Destination {} already exists. Deleting {}...", digestFile, firstFile);
            if (firstFile.delete()) {
                LOGGER.info("Deleted {}", firstFile);
            } else {
                LOGGER.error("Unable to delete {} for some reason.", firstFile);
            }
        }

        LOGGER.info("Creating entry in DB...");
        LOGGER.info("Log: {}", source);
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

            // TODO make relative to storage.
            LogRecord result = context.insertInto(LOG,
                    LOG.STOREDFILENAME,
                    LOG.URINAME,
                    LOG.TITLE,
                    LOG.UPLOADEDUTCMILLIS,
                    LOG.NOTES)
                    .values(digestFile.getAbsolutePath(),
                            source.getUriName(),
                            source.getTitle(),
                            source.getUploadedUtcMillis(),
                            source.getNotes())
                    .returning().fetchOne();
            LOGGER.info("...Created entry in DB.");
            LOGGER.info("Finished creating Log {} {}.", result.getId(), result.getStoredfilename());
            return R2L.map(result);
        } catch (SQLException ex) {
            throw new AppServerException("Failed to create log content: " + ex.getMessage(), ex);
        }
    }

    @Override
    public OutputStream getLogContent(long id) {
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
    public Log getLog(long id) {
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
    public List<Log> getLogs() {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
            RecordsToListHandler r2lHandler = new RecordsToListHandler();
            return context.selectFrom(LOG).fetchInto(r2lHandler).getLogs();
        } catch (SQLException ex) {
            throw new AppServerException("Cannot get logs list");
        }
    }

    @Override
    public void deleteLog(long id) {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

            context.delete(LOG).where(LOG.ID.eq(id)).execute();
        } catch (SQLException ex) {
            throw new AppServerException("Failed to delete log_id=" + id
                    + " because: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Log updateLog(Log source) {
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

    final protected static char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static File getTempFile() {
        try {
            return File.createTempFile("meterrier-upload-", ".tmp");
        } catch (IOException ex) {
            throw new AppServerException("Unable to create temporary file to store upload.", ex);
        }
    }

    private static File getDigestFile(String digestHex) {
        return new File("./meterrier-data/original-logs", digestHex);
    }

    private static class RecordToLogMapper implements RecordMapper<LogRecord, Log> {

        @Override
        public Log map(LogRecord record) {
            if (record == null) {
                return null;
            }
            return new Log(record.getId(),
                    record.getStoredfilename(),
                    record.getUriname(),
                    record.getTitle(),
                    record.getUploadedutcmillis(),
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
