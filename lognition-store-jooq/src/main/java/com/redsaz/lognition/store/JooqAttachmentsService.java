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
package com.redsaz.lognition.store;

import static com.redsaz.lognition.model.tables.Attachment.ATTACHMENT;

import com.redsaz.lognition.api.AttachmentsService;
import com.redsaz.lognition.api.exceptions.AppClientException;
import com.redsaz.lognition.api.exceptions.AppServerException;
import com.redsaz.lognition.api.model.Attachment;
import com.redsaz.lognition.model.tables.records.AttachmentRecord;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.InsertFinalStep;
import org.jooq.RecordMapper;
import org.jooq.SQLDialect;
import org.jooq.UpdateFinalStep;
import org.jooq.UpdateQuery;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores and accesses attachments.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class JooqAttachmentsService implements AttachmentsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JooqAttachmentsService.class);

  private static final RecordToAttachmentMapper R2A = new RecordToAttachmentMapper();

  private final DataSource dataSource;
  private final SQLDialect dialect;
  private final File attachmentsDir;

  /**
   * Create a new AttachmentsService backed by a data store.
   *
   * @param jdbcDataSource opens connections to database
   * @param sqlDialect the type of SQL database that we should speak
   * @param attachmentsDirectory where all of the attachments will be stored.
   */
  public JooqAttachmentsService(
      DataSource jdbcDataSource, SQLDialect sqlDialect, String attachmentsDirectory) {
    dataSource = jdbcDataSource;
    dialect = sqlDialect;
    attachmentsDir = new File(attachmentsDirectory);
    try {
      Files.createDirectories(attachmentsDir.toPath());
    } catch (IOException ex) {
      throw new RuntimeException("Unable to create data directories.", ex);
    }
  }

  @Override
  public Attachment put(Attachment source, InputStream data) {
    String owner = source.getOwner();
    String path = source.getPath();

    File uploadDestFile = writeTempDataFile(data);

    try (Connection c = dataSource.getConnection()) {
      DSLContext context = DSL.using(c, dialect);

      boolean exists =
          context.fetchExists(ATTACHMENT, ATTACHMENT.OWNER.eq(owner).and(ATTACHMENT.PATH.eq(path)));

      InsertFinalStep<AttachmentRecord> insert =
          context
              .insertInto(
                  ATTACHMENT,
                  ATTACHMENT.OWNER,
                  ATTACHMENT.PATH,
                  ATTACHMENT.NAME,
                  ATTACHMENT.DESCRIPTION,
                  ATTACHMENT.MIME_TYPE,
                  ATTACHMENT.UPLOADED_UTC_MILLIS)
              .values(
                  owner,
                  path,
                  source.getName(),
                  source.getDescription(),
                  source.getMimeType(),
                  source.getUploadedUtcMillis());

      Attachment attachment;
      if (!exists) {
        LOGGER.info("Creating DB entry for attachment owner={} path={}.", owner, path);
        insert.execute();
        attachment = get(context, owner, path);
        LOGGER.info(
            "Finished uploading attachment id={} owner={} path={}.",
            attachment.getId(),
            attachment.getOwner(),
            attachment.getPath());
      } else {
        LOGGER.info("owner={} path={} will replace old attachment.", owner, path);
        // If there is already an attachment with the same path for the same owner, then
        // use a transaction to delete that attachment so this attachment can take its
        // place. Once the transaction succeeds the attachment data can be deleted.
        Attachment deletedAttachment =
            context.transactionResult(
                configuration -> {
                  DSLContext ctx2 = DSL.using(configuration);
                  Attachment toDelete = get(ctx2, owner, path);
                  deleteRecord(ctx2, owner, path);
                  ctx2.execute(insert);

                  return toDelete;
                });
        if (deletedAttachment != null) {
          try {
            deleteFile(deletedAttachment.getId());
          } catch (AppServerException ex) {
            LOGGER.error(
                "Failed to delete attachmentId={} file for owner={} path={} after puting new attachment at the same path. This will need manually cleaned up.",
                deletedAttachment.getId(),
                deletedAttachment.getOwner(),
                deletedAttachment.getPath(),
                ex);
          }
        }
        AttachmentRecord result =
            context
                .selectFrom(ATTACHMENT)
                .where(ATTACHMENT.OWNER.eq(source.getOwner()), ATTACHMENT.PATH.eq(source.getPath()))
                .fetchOne();
        attachment = R2A.map(result);
      }
      File attachmentFile = new File(attachmentsDir, Long.toString(attachment.getId()));
      if (attachmentFile.exists()) {
        LOGGER.info("Replacing attachment data for " + attachment);
      }
      Files.move(
          uploadDestFile.toPath(),
          attachmentFile.toPath(),
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
      return attachment;
    } catch (SQLException ex) {
      throw new AppServerException(
          "Failed to create DB entry for attachment: " + ex.getMessage(), ex);
    } catch (IOException ex) {
      if (uploadDestFile.exists()) {
        uploadDestFile.delete();
      }
      throw new AppServerException("Failed to receive attachment: " + ex.getMessage(), ex);
    }
  }

  @Override
  public Attachment get(String owner, String path) {
    try (Connection c = dataSource.getConnection()) {
      DSLContext context = DSL.using(c, dialect);
      return get(context, owner, path);
    } catch (SQLException ex) {
      throw new AppServerException(
          "Error when looking for attachment for owner=" + owner + " path=" + path, ex);
    }
  }

  @Override
  public InputStream getData(String owner, String path) {
    Attachment attachment = null;
    try {
      attachment = get(owner, path);
      if (attachment == null) {
        throw new AppClientException(
            "No such attachment for owner=" + owner + " path=" + path + " exists.");
      }
      File attachmentFile = new File(attachmentsDir, Long.toString(attachment.getId()));
      return new BufferedInputStream(new FileInputStream(attachmentFile));
    } catch (IOException ex) {
      throw new AppServerException("Failed to open stream for " + attachment, ex);
    }
  }

  @Override
  public List<Attachment> listForOwner(String owner) {
    try (Connection c = dataSource.getConnection()) {
      DSLContext context = DSL.using(c, dialect);
      return context.selectFrom(ATTACHMENT).where(ATTACHMENT.OWNER.eq(owner)).fetch(R2A);
    } catch (SQLException ex) {
      throw new AppServerException("Cannot get attachments list because: " + ex.getMessage(), ex);
    }
  }

  @Override
  public void delete(String owner, String path) {
    try (Connection c = dataSource.getConnection()) {
      DSLContext context = DSL.using(c, dialect);
      Attachment attachment = get(context, owner, path);
      if (attachment == null) {
        throw new AppClientException(
            "No such attachment for owner=" + owner + " path=" + path + " exists.");
      }

      deleteFile(attachment.getId());
      deleteRecord(context, owner, path);
    } catch (SQLException ex) {
      throw new AppServerException(
          "Cannot delete attachment for owner="
              + owner
              + " path="
              + path
              + " because: "
              + ex.getMessage(),
          ex);
    }
  }

  @Override
  public void deleteForOwner(String owner) {
    listForOwner(owner).forEach(att -> delete(att.getOwner(), att.getPath()));
  }

  @Override
  public Attachment update(Attachment source) {
    try (Connection c = dataSource.getConnection()) {
      DSLContext context = DSL.using(c, dialect);

      UpdateQuery<AttachmentRecord> query = context.updateQuery(ATTACHMENT);
      if (source.getName() != null) {
        query.addValue(ATTACHMENT.NAME, source.getName());
      }
      if (source.getDescription() != null) {
        query.addValue(ATTACHMENT.DESCRIPTION, source.getDescription());
      }
      if (source.getMimeType() != null) {
        query.addValue(ATTACHMENT.MIME_TYPE, source.getMimeType());
      }
      query.addConditions(
          ATTACHMENT.OWNER.eq(source.getOwner()), ATTACHMENT.PATH.eq(source.getPath()));
      int numUpdated = query.execute();
      if (numUpdated != 1) {
        LOGGER.warn("{} attachment records updated (should be 1).", numUpdated);
      }
      AttachmentRecord result =
          context
              .selectFrom(ATTACHMENT)
              .where(ATTACHMENT.OWNER.eq(source.getOwner()), ATTACHMENT.PATH.eq(source.getPath()))
              .fetchOne();
      return R2A.map(result);
    } catch (SQLException ex) {
      throw new AppServerException("Error when updating " + source, ex);
    }
  }

  @Override
  public Attachment move(String owner, String sourcePath, String targetPath) {
    try (Connection c = dataSource.getConnection()) {
      DSLContext context = DSL.using(c, dialect);

      boolean sourceExists =
          context.fetchExists(
              ATTACHMENT, ATTACHMENT.OWNER.eq(owner).and(ATTACHMENT.PATH.eq(sourcePath)));

      if (!sourceExists) {
        throw new AppClientException(
            "Cannot move attachment owner="
                + owner
                + " path="
                + sourcePath
                + " because no attachment was found.");
      }

      boolean targetExists =
          context.fetchExists(
              ATTACHMENT, ATTACHMENT.OWNER.eq(owner).and(ATTACHMENT.PATH.eq(targetPath)));

      UpdateFinalStep<AttachmentRecord> updatePath =
          context
              .update(ATTACHMENT)
              .set(ATTACHMENT.PATH, targetPath)
              .where(ATTACHMENT.OWNER.eq(owner), ATTACHMENT.PATH.eq(sourcePath));

      if (targetExists) {
        // If there is already an attachment with the same path for the same owner, then
        // use a transaction to delete that attachment so this attachment can take its
        // place. Once the transaction succeeds the attachment data can be deleted.
        Attachment deletedAttachment =
            context.transactionResult(
                configuration -> {
                  DSLContext ctx2 = DSL.using(configuration);
                  Attachment toDelete = get(ctx2, owner, targetPath);
                  deleteRecord(ctx2, owner, targetPath);
                  ctx2.execute(updatePath);

                  return toDelete;
                });
        if (deletedAttachment != null) {
          try {
            deleteFile(deletedAttachment.getId());
          } catch (AppServerException ex) {
            LOGGER.error(
                "Failed to delete attachmentId={} file for owner={} path={} after moving attachment to that path. This will need manually cleaned up.",
                deletedAttachment.getId(),
                owner,
                targetPath,
                sourcePath,
                targetPath,
                ex);
          }
        }
      } else {
        int numUpdated = updatePath.execute();
        if (numUpdated != 1) {
          LOGGER.warn("{} attachment records updated (should be 1).", numUpdated);
        }
      }

      return get(context, owner, targetPath);
    } catch (SQLException ex) {
      throw new AppServerException(
          "Error when moving attachment for owner="
              + owner
              + " sourcePath="
              + sourcePath
              + " to targetPath="
              + targetPath,
          ex);
    }
  }

  //    @Override
  //    public Attachment get(long id) {
  //        try (Connection c = pool.getConnection()) {
  //            DSLContext context = DSL.using(c, dialect);
  //            return context.selectFrom(ATTACHMENT)
  //                    .where(ATTACHMENT.ID.eq(id))
  //                    .fetchOne(R2A);
  //        } catch (SQLException ex) {
  //            throw new AppServerException("Cannot get attachment_id=" + id + " because: " +
  // ex.getMessage(), ex);
  //        }
  //    }
  //    @Override
  //    public List<Attachment> getMany(Collection<Long> ids) {
  //        try (Connection c = pool.getConnection()) {
  //            DSLContext context = DSL.using(c, dialect);
  //            return context.selectFrom(ATTACHMENT)
  //                    .where(ATTACHMENT.ID.in(ids))
  //                    .fetch(R2A);
  //        } catch (SQLException ex) {
  //            throw new AppServerException("Cannot get attachment_ids=" + ids + " because: " +
  // ex.getMessage(), ex);
  //        }
  //    }
  //
  //    @Override
  //    public List<Attachment> listAll() {
  //        try (Connection c = pool.getConnection()) {
  //            DSLContext context = DSL.using(c, dialect);
  //            return context.selectFrom(ATTACHMENT).fetch(R2A);
  //        } catch (SQLException ex) {
  //            throw new AppServerException("Cannot get attachments list because: " +
  // ex.getMessage(), ex);
  //        }
  //    }
  //
  //    @Override
  //    public void delete(long id) {
  //        try (Connection c = pool.getConnection()) {
  //            DSLContext context = DSL.using(c, dialect);
  //            LOGGER.info("Deleting attachment_id={}...", id);
  //            File file = new File(Long.toString(id));
  //            if (!file.delete()) {
  //                LOGGER.error("Unable to delete attachment {}.", file);
  //            }
  //            int numDeleted = context.delete(ATTACHMENT).where(ATTACHMENT.ID.eq(id)).execute();
  //            if (numDeleted > 0) {
  //                LOGGER.info("...Deleted attachment_id={}.", id);
  //            } else {
  //                LOGGER.info("...No attachment_id={} was found in order to delete.");
  //            }
  //        } catch (SQLException ex) {
  //            throw new AppServerException("Failed to delete attachment_id=" + id
  //                    + " because: " + ex.getMessage(), ex);
  //        }
  //    }
  //
  //    @Override
  //    public Attachment update(Attachment source) {
  //        if (source == null) {
  //            throw new NullPointerException("No attachment information was specified.");
  //        }
  //
  //        LOGGER.debug("Updating entry in DB...");
  //        try (Connection c = pool.getConnection()) {
  //            DSLContext context = DSL.using(c, dialect);
  //
  //            UpdateSetMoreStep<AttachmentRecord> up =
  // context.update(ATTACHMENT).set(ATTACHMENT.ID, source.getId());
  //            if (source.getPath() != null) {
  //                up.set(ATTACHMENT.PATHNAME, source.getPath());
  //            }
  //            if (source.getDescription() != null) {
  //                up.set(ATTACHMENT.DESCRIPTION, source.getDescription());
  //            }
  //            if (source.getUploadedUtcMillis() != 0) {
  //                up.set(IMPORT_INFO.UPLOADED_UTC_MILLIS, source.getUploadedUtcMillis());
  //            }
  //            AttachmentRecord result =
  // up.where(ATTACHMENT.ID.eq(source.getId())).returning().fetchOne();
  //            LOGGER.debug("...Updated entry in DB.");
  //            return R2A.map(result);
  //        } catch (SQLException ex) {
  //            throw new AppServerException("Failed to update attachment: " + ex.getMessage(), ex);
  //        }
  //    }
  private File writeTempDataFile(InputStream data) {
    if (data == null) {
      throw new NullPointerException("No attachment data provided.");
    }

    LOGGER.info("Receiving attachment...");
    long bytesRead = 0;
    File uploadDestFile = createUploadFile();
    LOGGER.info("Storing into {}", uploadDestFile.getAbsolutePath());
    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(uploadDestFile))) {
      byte[] buff = new byte[4096];
      int num;
      while ((num = data.read(buff)) >= 0) {
        os.write(buff, 0, num);
        bytesRead += num;
      }
      os.flush();
    } catch (IOException ex) {
      LOGGER.error("Exception when uploading attachment.", ex);
      throw new AppServerException("Failed to upload content.", ex);
    }
    if (bytesRead == 0) {
      uploadDestFile.delete();
      throw new AppClientException("No data was uploaded.");
    }
    LOGGER.info("...Stored {} bytes into file {}.", bytesRead, uploadDestFile.getAbsolutePath());

    return uploadDestFile;
  }

  private Attachment get(DSLContext context, String owner, String path) {
    AttachmentRecord result =
        context
            .selectFrom(ATTACHMENT)
            .where(ATTACHMENT.OWNER.eq(owner), ATTACHMENT.PATH.eq(path))
            .fetchOne();
    return R2A.map(result);
  }

  private File createUploadFile() {
    try {
      return File.createTempFile("log-", ".tmp", attachmentsDir);
    } catch (IOException ex) {
      LOGGER.error("Exception when creating upload file.", ex);
      throw new AppServerException("Failed to upload content.", ex);
    }
  }

  private void deleteFile(long attachmentId) {
    // Delete the data first, then the record.
    File attachmentFile = new File(attachmentsDir, Long.toString(attachmentId));
    if (attachmentFile.exists()) {
      if (!attachmentFile.delete()) {
        throw new AppServerException(
            "Cannot delete attachmentId="
                + attachmentId
                + " because "
                + attachmentFile
                + " could not be deleted.");
      }
    } else {
      LOGGER.warn("Attachment data does not exist for attachmentId=" + attachmentId + ".");
    }
  }

  private int deleteRecord(DSLContext context, String owner, String path) {
    int numDeleted =
        context
            .deleteFrom(ATTACHMENT)
            .where(ATTACHMENT.OWNER.eq(owner), ATTACHMENT.PATH.eq(path))
            .execute();
    if (numDeleted == 0) {
      LOGGER.info("No records erased for attachment for owner=" + owner + " path=" + path);
    } else {
      LOGGER.info("Deleted {} attachment record for owner={} path={}", numDeleted, owner, path);
    }
    return numDeleted;
  }

  private static class RecordToAttachmentMapper
      implements RecordMapper<AttachmentRecord, Attachment> {

    @Override
    public Attachment map(AttachmentRecord record) {
      if (record == null) {
        return null;
      }
      return new Attachment(
          record.getId(),
          record.getOwner(),
          record.getPath(),
          record.getName(),
          record.getDescription(),
          record.getMimeType(),
          record.getUploadedUtcMillis());
    }
  }

  //    private static class AttachmentRecordsToListHandler implements
  // RecordHandler<AttachmentRecord> {
  //
  //        private final List<Attachment> attachments = new ArrayList<>();
  //
  //        @Override
  //        public void next(AttachmentRecord record) {
  //            attachments.add(R2A.map(record));
  //        }
  //
  //        public List<Attachment> getAttachments() {
  //            return attachments;
  //        }
  //    }
}
