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
package com.redsaz.debitage.store;

import com.redsaz.debitage.api.exceptions.AppServerException;
import com.redsaz.debitage.api.model.Log;
import com.redsaz.debitage.model.tables.records.LogRecord;
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
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hsqldb.jdbc.JDBCPool;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import com.redsaz.debitage.api.LogsService;
import static com.redsaz.debitage.model.tables.Log.LOG;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jooq.RecordHandler;
import org.jooq.RecordMapper;

/**
 * Stores and accesses logs.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class HsqlLogsService implements LogsService {

    private static final Logger LOGGER = Logger.getLogger(HsqlLogsService.class.getName());

    private static final JDBCPool POOL = initPool();

    private static final RecordToLogMapper r2lMapper = new RecordToLogMapper();

//    @Override
//    public List<LogBrief> getLogBriefs() {
//        try (Connection c = POOL.getConnection()) {
//            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
//            List<LogBrief> briefRecords = context.selectFrom(BRIEF).fetch().into(LogBrief.class);
//            return briefRecords;
//        } catch (SQLException ex) {
//            throw new AppServerException("Cannot retrieve log briefs: " + ex.getMessage(), ex);
//        }
//    }
//
//    @Override
//    public LogBrief getLogBrief(long id) {
//        try (Connection c = POOL.getConnection()) {
//            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
//            LogBrief mbr = context.selectFrom(BRIEF).where(BRIEF.ID.eq(id)).fetchOneInto(LogBrief.class);
//            return mbr;
//        } catch (SQLException ex) {
//            throw new AppServerException("Cannot retrieve log briefs: " + ex.getMessage(), ex);
//        }
//    }
//
//    @Override
//    public OutputStream getLog(long id) {
////        try (Connection c = POOL.getConnection()) {
////            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
////
////            LogRecord nr = context.selectFrom(LOG).where(LOG.ID.eq(id)).fetchOne();
////            return recordToLog(nr);
////        } catch (SQLException ex) {
////            throw new AppServerException("Cannot get log_id=" + id + " because: " + ex.getMessage(), ex);
////        }
//        return null;
//    }
//
//    @Override
//    public LogBrief createBrief(LogBrief source) {
//        if (source == null) {
//            throw new NullPointerException("No log brief was specified.");
//        }
//        try (Connection c = POOL.getConnection()) {
//            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
//
//            BriefRecord mbr = context.newRecord(BRIEF, source);
//            context.executeInsert(mbr);
//            return null;
//        } catch (SQLException ex) {
//            throw new AppServerException("Failed to create log brief: " + ex.getMessage(), ex);
//        }
//    }
    @Override
    public Log createLog(InputStream raw) {
        if (raw == null) {
            throw new NullPointerException("No log was specified.");
        }

        LOGGER.info("Storing log file...");
        File firstFile = getRandomFile();
        LOGGER.log(Level.INFO, "Storing initially into {0}", firstFile.getAbsolutePath());
        File digestFile;
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(firstFile))) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buff = new byte[256];
            int num;
            while ((num = raw.read(buff)) >= 0) {
                md.update(buff, 0, num);
                os.write(buff);
            }
            os.flush();
            String digestHex = bytesToHex(md.digest());
            digestFile = getDigestFile(digestHex);
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new AppServerException("Failed to upload content.", ex);
        }
        if (!Files.exists(digestFile.toPath())) {
            try {
                LOGGER.log(Level.INFO, "Moving content to permanent home {0}...", digestFile);
                Files.move(firstFile.toPath(), digestFile.toPath());
                LOGGER.log(Level.INFO, "Moved  content to permanent home {0}...", digestFile);
            } catch (IOException ex) {
                firstFile.delete(); // Assume the move failed so delete original.
                throw new AppServerException("Failed to upload content.", ex);
            }
        } else {
            LOGGER.log(Level.INFO, String.format("Destination %s already exists. Deleting %s...", digestFile, firstFile));
            if (digestFile.delete()) {
                LOGGER.log(Level.INFO, "Deleted {0}");
            } else {
                LOGGER.log(Level.SEVERE, "Unable to delete {0} for some reason.", firstFile);
            }
        }

        LOGGER.log(Level.INFO, "Creating entry in DB...");
        try (Connection c = POOL.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

            // TODO make relative to storage.
            LogRecord logRec = new LogRecord(null, digestFile.getAbsolutePath());
            context.executeInsert(logRec);
            LOGGER.log(Level.INFO, "Created entry in DB. Retrieving from DB...");
            Log result = context.selectFrom(LOG)
                    .where(LOG.STOREDFILENAME.eq(logRec.getStoredfilename()))
                    .fetchOne(r2lMapper);
            LOGGER.log(Level.INFO, "Finished creating Log {0} {1}.", new Object[]{result.getId(), result.getStoredFilename()});
            return result;
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
        try (Connection c = POOL.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
            return context.selectFrom(LOG)
                    .where(LOG.ID.eq(id))
                    .fetchOne(r2lMapper);
        } catch (SQLException ex) {
            throw new AppServerException("Cannot get log_id=" + id + " because: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Log> getLogs() {
        try (Connection c = POOL.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
            RecordsToListHandler r2lHandler = new RecordsToListHandler();
            return context.selectFrom(LOG).fetchInto(r2lHandler).getLogs();
        } catch (SQLException ex) {
            throw new AppServerException("Cannot get logs list");
        }
    }

    @Override
    public void deleteLog(long id) {
        try (Connection c = POOL.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

            context.delete(LOG).where(LOG.ID.eq(id)).execute();
        } catch (SQLException ex) {
            throw new AppServerException("Failed to delete log_id=" + id
                    + " because: " + ex.getMessage(), ex);
        }
    }

    private static JDBCPool initPool() {
        System.out.println("Initing DB...");
        File deciDir = new File("./debitage");
        if (!deciDir.exists() && !deciDir.mkdirs()) {
            throw new RuntimeException("Could not create " + deciDir);
        }
        File deciDb = new File(deciDir, "debitagedb");
        JDBCPool jdbc = new JDBCPool();
        jdbc.setUrl("jdbc:hsqldb:" + deciDb.toURI());
        jdbc.setUser("SA");
        jdbc.setPassword("SA");

        try (Connection c = jdbc.getConnection()) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
            Liquibase liquibase = new Liquibase("debitage-db.yaml", new ClassLoaderResourceAccessor(), database);
            liquibase.update((String) null);
        } catch (SQLException | LiquibaseException ex) {
            throw new AppServerException("Cannot initialize logs service: " + ex.getMessage(), ex);
        }
        return jdbc;
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

    private static File getRandomFile() {
        try {
            return File.createTempFile("temporary", ".tmp");
        } catch (IOException ex) {
            throw new AppServerException("Failed to upload content: " + ex.getMessage(), ex);
        }
    }

    private static File getDigestFile(String digestHex) {
        try {
            return File.createTempFile(digestHex, ".tmp");
        } catch (IOException ex) {
            throw new AppServerException("Failed to upload content: " + ex.getMessage(), ex);
        }
    }

    private static class RecordToLogMapper implements RecordMapper<LogRecord, Log> {

        @Override
        public Log map(LogRecord record) {
            return new Log(record.getId(), record.getStoredfilename());
        }
    }

    private static class RecordsToListHandler implements RecordHandler<LogRecord> {

        private final List<Log> logs = new ArrayList<>();

        @Override
        public void next(LogRecord record) {
            LOGGER.log(Level.INFO, "Retrieved {0} Log record.", record.getId());
            logs.add(r2lMapper.map(record));
        }

        public List<Log> getLogs() {
            return logs;
        }
    }
}
