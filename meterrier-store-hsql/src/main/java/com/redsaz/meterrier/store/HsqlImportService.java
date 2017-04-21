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

import com.redsaz.meterrier.api.ImportService;
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
import static com.redsaz.meterrier.model.tables.Pendingimport.PENDINGIMPORT;
import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.redsaz.meterrier.api.model.ImportInfo;
import com.redsaz.meterrier.model.tables.records.PendingimportRecord;
import java.util.ArrayList;
import org.jooq.RecordHandler;
import org.jooq.RecordMapper;
import org.jooq.UpdateSetMoreStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores and accesses log imports.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class HsqlImportService implements ImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HsqlImportService.class);

    private static final RecordToImportMapper R2I = new RecordToImportMapper();

    private final JDBCPool pool;

    /**
     * Create a new HSQLDB-base ImportService.
     *
     * @param jdbcPool opens connections to database
     */
    public HsqlImportService(JDBCPool jdbcPool) {
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
    public ImportInfo upload(InputStream raw, ImportInfo source) {
        if (raw == null) {
            throw new NullPointerException("No import was specified.");
        } else if (source == null) {
            throw new NullPointerException("No import information was specified.");
        }

        LOGGER.info("Storing uploaded file...");
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
            LOGGER.error("Exception when uploading log.", ex);
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
        LOGGER.info("Import: {}", source);
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

            // TODO make relative to storage.
            PendingimportRecord result = context.insertInto(PENDINGIMPORT,
                    PENDINGIMPORT.IMPORTEDFILENAME,
                    PENDINGIMPORT.TITLE,
                    PENDINGIMPORT.USERSPECIFIEDTYPE,
                    PENDINGIMPORT.UPLOADEDUTCMILLIS)
                    .values(digestFile.getAbsolutePath(),
                            source.getTitle(),
                            source.getUserSpecifiedType(),
                            source.getUploadedUtcMillis())
                    .returning().fetchOne();
            LOGGER.info("...Created entry in DB.");
            LOGGER.info("Finished uploading import {} {}.", result.getId(), result.getImportedfilename());
            return R2I.map(result);
        } catch (SQLException ex) {
            throw new AppServerException("Failed to create import: " + ex.getMessage(), ex);
        }
    }

    @Override
    public ImportInfo get(long id) {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
            return context.selectFrom(PENDINGIMPORT)
                    .where(PENDINGIMPORT.ID.eq(id))
                    .fetchOne(R2I);
        } catch (SQLException ex) {
            throw new AppServerException("Cannot get import_id=" + id + " because: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<ImportInfo> list() {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
            PendingimportRecordsToListHandler r2iHandler = new PendingimportRecordsToListHandler();
            return context.selectFrom(PENDINGIMPORT).fetchInto(r2iHandler).getImports();
        } catch (SQLException ex) {
            throw new AppServerException("Cannot get imports list because: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(long id) {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
            LOGGER.info("Deleting import_id={}...", id);
            ImportInfo info = get(id);
            if (info == null) {
                LOGGER.info("No such import_id={} exists.", id);
                return;
            }
            File file = new File(info.getImportedFilename());
            if (!file.delete()) {
                LOGGER.error("Unable to delete imported file {}.", file);
            }
            context.delete(PENDINGIMPORT).where(PENDINGIMPORT.ID.eq(id)).execute();
            LOGGER.info("...Finished deleting import_id{}.", id);
        } catch (SQLException ex) {
            throw new AppServerException("Failed to delete pendingimport_id=" + id
                    + " because: " + ex.getMessage(), ex);
        }
    }

    @Override
    public ImportInfo update(ImportInfo source) {
        if (source == null) {
            throw new NullPointerException("No import information was specified.");
        }

        LOGGER.info("Updating entry in DB...");
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

            UpdateSetMoreStep<PendingimportRecord> up = context.update(PENDINGIMPORT).set(PENDINGIMPORT.ID, source.getId());
            if (source.getImportedFilename() != null) {
                up.set(PENDINGIMPORT.IMPORTEDFILENAME, source.getImportedFilename());
            }
            if (source.getTitle() != null) {
                up.set(PENDINGIMPORT.TITLE, source.getTitle());
            }
            if (source.getUserSpecifiedType() != null) {
                up.set(PENDINGIMPORT.USERSPECIFIEDTYPE, source.getUserSpecifiedType());
            }
            if (source.getUploadedUtcMillis() != 0) {
                up.set(PENDINGIMPORT.UPLOADEDUTCMILLIS, source.getUploadedUtcMillis());
            }
            PendingimportRecord result = up.where(PENDINGIMPORT.ID.eq(source.getId())).returning().fetchOne();
            LOGGER.info("...Updated entry in DB.");
            return R2I.map(result);
        } catch (SQLException ex) {
            throw new AppServerException("Failed to update import: " + ex.getMessage(), ex);
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

    private static class RecordToImportMapper implements RecordMapper<PendingimportRecord, ImportInfo> {

        @Override
        public ImportInfo map(PendingimportRecord record) {
            if (record == null) {
                return null;
            }
            return new ImportInfo(record.getId(),
                    record.getImportedfilename(),
                    record.getTitle(),
                    record.getUserspecifiedtype(),
                    record.getUploadedutcmillis(),
                    null
            );
        }
    }

    private static class PendingimportRecordsToListHandler implements RecordHandler<PendingimportRecord> {

        private final List<ImportInfo> imports = new ArrayList<>();

        @Override
        public void next(PendingimportRecord record) {
            imports.add(R2I.map(record));
        }

        public List<ImportInfo> getImports() {
            return imports;
        }
    }
}
