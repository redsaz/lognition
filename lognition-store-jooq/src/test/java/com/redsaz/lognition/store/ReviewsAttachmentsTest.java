/*
 * Copyright 2021 Redsaz <redsaz@gmail.com>.
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

import com.redsaz.lognition.api.exceptions.AppClientException;
import com.redsaz.lognition.api.exceptions.AppServerException;
import com.redsaz.lognition.api.model.Attachment;
import com.redsaz.lognition.api.model.Review;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.hsqldb.jdbc.JDBCPool;
import org.jooq.SQLDialect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for the attachment functionality of the reviews service.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class ReviewsAttachmentsTest {

    @Rule
    public TemporaryFolder attachTempDir = new TemporaryFolder();

    @Test
    public void testCreateAndDownloadAttachment() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool();
                DigestInputStream data = attachmentData()) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);

            // Given an existing review,
            Review review = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review = reviewsSvc.create(review);

            // When an attachment is uploaded and the attachment and content is retrieved,
            String path = "path.jpg";
            Attachment source = new Attachment(0L, "", path, "Image", "It is an image",
                    "image/jpeg", System.currentTimeMillis());
            Attachment actual = reviewsSvc.putAttachment(review.getId(), source, data);
            DigestInputStream actualData = wrapInDigestStream(
                    reviewsSvc.getAttachmentData(review.getId(), source.getPath()));

            // Then the downloaded data should match what was uploaded,
            assertEqualData(data, actualData);
            // and the attachment details should be what was given.
            assertEquals("Path", source.getPath(), actual.getPath());
            assertEquals("Name", source.getName(), actual.getName());
            assertEquals("Description", source.getDescription(), actual.getDescription());
            assertEquals("MimeType", source.getMimeType(), actual.getMimeType());

            // (And also, implementation details:)
            // The filename on disk should not be identical to the path of the attachment, as that
            // seems like a bad idea.
            assertFalse("Must not store the file with the actual filename of the attachment.",
                    Files.exists(new File(attachDir, path).toPath()));

            // The resulting attachment details should have a non-zero id and should have an owner
            // that matches the review-id.
            assertEquals("Attachment Id", 1, actual.getId());
            assertEquals("Owner", "reviews/1", actual.getOwner());
        }
    }

    @Test
    public void testCreateAttachment_existingName() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool();
                DigestInputStream preData = attachmentData();
                DigestInputStream data = attachmentData("gray.jpg")) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);

            // Given an existing review with an attachment,
            Review review = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review = reviewsSvc.create(review);
            Attachment preSource = new Attachment(0L, "", "path.jpg", "Image", "It is an image",
                    "image/jpeg", System.currentTimeMillis());
            Attachment preActual = reviewsSvc.putAttachment(review.getId(), preSource, preData);

            // When another attachment is uploaded with the same path as the original attachment,
            Attachment source = new Attachment(0L, "", "path.jpg", "Image2", "It is a different image",
                    "image/jpeg", System.currentTimeMillis());
            Attachment actual = reviewsSvc.putAttachment(review.getId(), source, data);
            DigestInputStream actualData = wrapInDigestStream(
                    reviewsSvc.getAttachmentData(review.getId(), source.getPath()));

            // Then the new attachment is stored and accessible at that path,
            assertEqualData(data, actualData);

            assertEquals("Path", source.getPath(), actual.getPath());
            assertEquals("Name", source.getName(), actual.getName());
            assertEquals("Description", source.getDescription(), actual.getDescription());
            assertEquals("MimeType", source.getMimeType(), actual.getMimeType());

            // (impelemntation details) and the old attachment is deleted.
            assertEquals("Attachment Id", 2, actual.getId());
            assertEquals("Owner", "reviews/1", actual.getOwner());
            assertEquals("One file should exist after adding another attachment with same path",
                    Files.list(attachDir.toPath()).count(), 1L);
        }
    }

    @Test
    public void testCreateAttachment_samePathDifferentReviews() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool();
                DigestInputStream data1 = attachmentData();
                DigestInputStream data2 = attachmentData("gray.jpg")) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);

            // Given two existing reviews,
            Review review1 = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review1 = reviewsSvc.create(review1);
            Review review2 = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review2 = reviewsSvc.create(review2);

            // When attachments are uploaded to both, each with the same pathname, and the details
            // and contents are retrieved,
            Attachment source1 = new Attachment(0L, "", "path.jpg", "Image-first", "It is an original image", "image/jpeg", System.currentTimeMillis());
            Attachment actual1 = reviewsSvc.putAttachment(review1.getId(), source1, data1);
            actual1 = reviewsSvc.getAttachment(review1.getId(), source1.getPath());

            Attachment source2 = new Attachment(0L, "", "path.jpg", "Image-second", "It is an unoriginal image", "image/jpeg", System.currentTimeMillis());
            Attachment actual2 = reviewsSvc.putAttachment(review2.getId(), source2, data2);
            actual2 = reviewsSvc.getAttachment(review2.getId(), source2.getPath());

            // Then both attachments should still exist because they are owned by different reviews
            // and therefore have different URLs.
            DigestInputStream actualData1 = wrapInDigestStream(
                    reviewsSvc.getAttachmentData(review1.getId(), source1.getPath()));
            assertEqualData(data1, actualData1);
            assertEquals("Path", source1.getPath(), actual1.getPath());
            assertEquals("Name", source1.getName(), actual1.getName());
            assertEquals("Description", source1.getDescription(), actual1.getDescription());
            assertEquals("MimeType", source1.getMimeType(), actual1.getMimeType());

            DigestInputStream actualData2 = wrapInDigestStream(
                    reviewsSvc.getAttachmentData(review2.getId(), source2.getPath()));
            assertEqualData(data2, actualData2);
            assertEquals("Path", source2.getPath(), actual2.getPath());
            assertEquals("Name", source2.getName(), actual2.getName());
            assertEquals("Description", source2.getDescription(), actual2.getDescription());
            assertEquals("MimeType", source2.getMimeType(), actual2.getMimeType());
        }
    }

    @Test
    public void testUpdateAttachment() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool();
                DigestInputStream data = attachmentData()) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);

            // Given an existing review with an attachment,
            Review review = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review = reviewsSvc.create(review);
            Attachment source = new Attachment(0L, "", "path.jpg", "Image", "It is an image", "image/jpeg", System.currentTimeMillis());
            Attachment actual = reviewsSvc.putAttachment(review.getId(), source, data);

            // When the name, description, and/or mimetype change,
            source = new Attachment(actual.getId(), actual.getOwner(), actual.getPath(), "Updated Name", "Updated Description", "image", actual.getUploadedUtcMillis());
            actual = reviewsSvc.updateAttachment(review.getId(), source);

            // Then those changes should be from the returned info from the update,
            assertEquals("Path", source.getPath(), actual.getPath());
            assertEquals("Name", source.getName(), actual.getName());
            assertEquals("Description", source.getDescription(), actual.getDescription());
            assertEquals("MimeType", source.getMimeType(), actual.getMimeType());

            // and when getting the attachment.
            actual = reviewsSvc.getAttachment(review.getId(), actual.getPath());
            assertEquals("Path", source.getPath(), actual.getPath());
            assertEquals("Name", source.getName(), actual.getName());
            assertEquals("Description", source.getDescription(), actual.getDescription());
            assertEquals("MimeType", source.getMimeType(), actual.getMimeType());
        }
    }

    @Test
    public void testUpdateAttachment_nullsShouldNotChange() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool();
                DigestInputStream data = attachmentData()) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);

            // Given an existing review with an attachment,
            Review review = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review = reviewsSvc.create(review);
            Attachment sourceFirst = new Attachment(0L, "", "path.jpg", "Image", "It is an image", "image/jpeg", System.currentTimeMillis());
            Attachment actual = reviewsSvc.putAttachment(review.getId(), sourceFirst, data);

            // When the name, description, and/or mimetype are given nulls,
            Attachment sourceWithNulls = new Attachment(actual.getId(), actual.getOwner(), actual.getPath(), null, null, null, actual.getUploadedUtcMillis());
            actual = reviewsSvc.updateAttachment(review.getId(), sourceWithNulls);

            // Then those fields should not be changed,
            assertEquals("Path", sourceFirst.getPath(), actual.getPath());
            assertEquals("Name", sourceFirst.getName(), actual.getName());
            assertEquals("Description", sourceFirst.getDescription(), actual.getDescription());
            assertEquals("MimeType", sourceFirst.getMimeType(), actual.getMimeType());

            // and when getting the attachment.
            actual = reviewsSvc.getAttachment(review.getId(), actual.getPath());
            assertEquals("Path", sourceFirst.getPath(), actual.getPath());
            assertEquals("Name", sourceFirst.getName(), actual.getName());
            assertEquals("Description", sourceFirst.getDescription(), actual.getDescription());
            assertEquals("MimeType", sourceFirst.getMimeType(), actual.getMimeType());
        }
    }

    @Test
    public void testUpdateAttachment_cannotUpdateUnchangeableFields() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool();
                DigestInputStream data = attachmentData()) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);

            // Given an existing review with an attachment,
            Review review = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review = reviewsSvc.create(review);
            Attachment source = new Attachment(0L, "", "path.jpg", "Image", "It is an image", "image/jpeg", System.currentTimeMillis());
            Attachment original = reviewsSvc.putAttachment(review.getId(), source, data);

            // When the attachment id or uploaded fields are attempted to be updated,
            Attachment changed = new Attachment(1234L, original.getOwner(), original.getPath(), null, null, null, 4321L);
            Attachment result = reviewsSvc.updateAttachment(review.getId(), changed);

            // Then those fields should not change from the original upload.
            assertEquals("Id", original.getId(), result.getId());
            assertEquals("UploadedUtcMillis", original.getUploadedUtcMillis(), result.getUploadedUtcMillis());

            // and when getting the attachment.
            result = reviewsSvc.getAttachment(review.getId(), original.getPath());
            assertEquals("Id", original.getId(), result.getId());
            assertEquals("UploadedUtcMillis", original.getUploadedUtcMillis(), result.getUploadedUtcMillis());
        }
    }

    @Test
    public void testMoveAttachment() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool();
                DigestInputStream data = attachmentData()) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);

            // Given an existing review with an attachment,
            Review review = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review = reviewsSvc.create(review);
            Attachment source = new Attachment(0L, "", "path.jpg", "Image", "It is an image", "image/jpeg", System.currentTimeMillis());
            Attachment actual = reviewsSvc.putAttachment(review.getId(), source, data);

            // When the attachment is moved to a different path,
            String movedPath = "moved.jpg";
            actual = reviewsSvc.moveAttachment(review.getId(), actual.getPath(), movedPath);

            // Then the new path should be reflected in the attachment details from the result with
            // other data unchanged,
            assertEquals("Path", movedPath, actual.getPath());
            assertEquals("Name", source.getName(), actual.getName());
            assertEquals("Description", source.getDescription(), actual.getDescription());
            assertEquals("MimeType", source.getMimeType(), actual.getMimeType());
            // and likewise when getting the attachment,
            actual = reviewsSvc.getAttachment(review.getId(), actual.getPath());
            assertEquals("Path", movedPath, actual.getPath());
            assertEquals("Name", source.getName(), actual.getName());
            assertEquals("Description", source.getDescription(), actual.getDescription());
            assertEquals("MimeType", source.getMimeType(), actual.getMimeType());

            // and the data is downloadable from the new path,
            DigestInputStream actualData = wrapInDigestStream(
                    reviewsSvc.getAttachmentData(review.getId(), actual.getPath()));
            assertEqualData(data, actualData);

            // and the attachment is not retrievable from the original path.
            assertNull("Attachment should not be retrievable from original path.", reviewsSvc.getAttachment(review.getId(), source.getPath()));
        }
    }

    @Test
    public void testMoveAttachment_overwritesOtherAttachment() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool();
                DigestInputStream data1 = attachmentData();
                DigestInputStream data2 = attachmentData()) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);

            // Given an existing review with two attachments,
            Review review = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review = reviewsSvc.create(review);
            Attachment source1 = new Attachment(0L, "", "path1.jpg", "Image1", "It is an image1", "image/jpeg", System.currentTimeMillis());
            Attachment actual1 = reviewsSvc.putAttachment(review.getId(), source1, data1);
            Attachment source2 = new Attachment(0L, "", "path2.jpg", "Image2", "It is an image2", "image/jpeg", System.currentTimeMillis());
            Attachment actual2 = reviewsSvc.putAttachment(review.getId(), source2, data2);

            // When one attachment is moved to the same path as the other attachment,
            String movedPath = actual2.getPath();
            actual1 = reviewsSvc.moveAttachment(review.getId(), actual1.getPath(), movedPath);

            // Then only that one attachment exists,
            assertEquals("Path", movedPath, actual1.getPath());
            assertEquals("Name", source1.getName(), actual1.getName());
            assertEquals("Description", source1.getDescription(), actual1.getDescription());
            assertEquals("MimeType", source1.getMimeType(), actual1.getMimeType());
            // and likewise when getting the attachment,
            actual1 = reviewsSvc.getAttachment(review.getId(), actual1.getPath());
            assertEquals("Path", movedPath, actual1.getPath());
            assertEquals("Name", source1.getName(), actual1.getName());
            assertEquals("Description", source1.getDescription(), actual1.getDescription());
            assertEquals("MimeType", source1.getMimeType(), actual1.getMimeType());

            // and the data is downloadable from the new path,
            DigestInputStream actualData = wrapInDigestStream(
                    reviewsSvc.getAttachmentData(review.getId(), actual1.getPath()));
            assertEqualData(data1, actualData);

            // and the attachment is not retrievable from the original path.
            assertNull("Attachment should not be retrievable from original path.", reviewsSvc.getAttachment(review.getId(), source1.getPath()));

            // (impl details) and the old attachment is deleted.
            assertEquals("One file should exist after adding another attachment with same path",
                    Files.list(attachDir.toPath()).count(), 1L);
        }
    }

    @Test(expected = AppClientException.class)
    public void testMoveAttachment_noSourceAttachment() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool()) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);

            // Given an existing review with NO attachment,
            Review review = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review = reviewsSvc.create(review);

            // When attempting to move a non-existing attachment,
            reviewsSvc.moveAttachment(review.getId(), "bogus-src", "bogus-dest");

            // Then an exception is thrown indicating the attachment does not exist to move.
        }
    }

    @Test
    public void testDeleteAttachment() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool();
                DigestInputStream data = attachmentData()) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);
            String path = "path.jpg";

            // Given an existing review with an attachment,
            Review review = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review = reviewsSvc.create(review);
            Attachment source = new Attachment(0L, "", path, "Image", "It is an image",
                    "image/jpeg", System.currentTimeMillis());
            Attachment actual = reviewsSvc.putAttachment(review.getId(), source, data);

            // When the attachment is deleted,
            reviewsSvc.deleteAttachment(review.getId(), path);

            // Then the attachment cannot be looked up and the stored file is deleted from disk.
            actual = reviewsSvc.getAttachment(review.getId(), path);
            assertNull("Looking up a deleted attachment should return null.", actual);

            try {
                reviewsSvc.getAttachmentData(review.getId(), path);
                fail("Exception should be thrown when attempting to fetch data for attachment that was deleted.");
            } catch (AppClientException ex) {
                // Good, exception should be thrown here.
            }

            // (impelemntation details) and the old attachment is deleted.
            assertEquals("The attachment file should be deleted when the attachment was deleted.",
                    Files.list(attachDir.toPath()).count(), 0L);
        }
    }

    @Test(expected = AppClientException.class)
    public void testDeleteAttachment_noAttachment() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool()) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);

            // Given an existing review,
            Review review = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review = reviewsSvc.create(review);

            // When attempting to delete a non-existent attachment,
            reviewsSvc.deleteAttachment(review.getId(), "path.jpg");

            // Then an exception is thrown because one can't delete something that isn't there.
            // (though, I dunno. Success can also make sense, because the attachment isn't, which
            // is the point of deleting the attachment.)
        }
    }

    @Test
    public void testDeleteReview() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool();
                DigestInputStream data = attachmentData()) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);
            String path = "path.jpg";

            // Given an existing review with an attachment,
            Review review = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review = reviewsSvc.create(review);
            Attachment source = new Attachment(0L, "", path, "Image", "It is an image", "image/jpeg", System.currentTimeMillis());
            Attachment actual = reviewsSvc.putAttachment(review.getId(), source, data);

            // When the review is deleted,
            reviewsSvc.delete(review.getId());

            // Then the review is no longer available, and neither is the attachment, and the
            // attachment file is deleted.
            assertNull("Deleted review should not be look-up-able.", reviewsSvc.get(review.getId()));
            actual = reviewsSvc.getAttachment(review.getId(), path);
            assertNull("Looking up a deleted attachment should return null.", actual);

            try {
                reviewsSvc.getAttachmentData(review.getId(), path);
                fail("Exception should be thrown when attempting to fetch data for attachment that was deleted.");
            } catch (AppClientException ex) {
                // Good, exception should be thrown here.
            }

            // (impl details) and the old attachment is deleted.
            assertEquals("The attachment file should be deleted when the attachment was deleted.",
                    Files.list(attachDir.toPath()).count(), 0L);
        }
    }

    @Test
    public void testListAttachments() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool();
                DigestInputStream data = attachmentData()) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);
            String path = "path.jpg";

            // Given an existing review with an attachment,
            Review review = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review = reviewsSvc.create(review);
            Attachment source = new Attachment(0L, "", path, "Image", "It is an image", "image/jpeg", System.currentTimeMillis());
            Attachment expected = reviewsSvc.putAttachment(review.getId(), source, data);

            // When listing attachments for a review,
            List<Attachment> attachments = reviewsSvc.listAttachments(review.getId());

            // Then the attachment should show up in the list.
            assertEquals("# of attachments for review", 1, attachments.size());
            assertEquals(expected, attachments.get(0));
        }
    }

    @Test
    public void testListAttachments_empty() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool()) {
            File attachDir = attachTempDir.newFolder();
            JooqAttachmentsService attSvc = new JooqAttachmentsService(
                    cp, SQLDialect.HSQLDB, attachDir.toString());
            JooqReviewsService reviewsSvc = new JooqReviewsService(cp, SQLDialect.HSQLDB, attSvc);

            // Given an existing review with no attachment,
            Review review = new Review(0, "test", "Test", "Description", System.currentTimeMillis(),
                    System.currentTimeMillis(), "label");
            review = reviewsSvc.create(review);

            // When listing attachments for the review,
            List<Attachment> attachments = reviewsSvc.listAttachments(review.getId());

            // Then an empty list is returned.
            assertTrue("An empty attachment list should be returned.", attachments.isEmpty());
        }
    }

    private static DigestInputStream attachmentData() throws IOException {
        return attachmentData("image.jpg");
    }

    private static DigestInputStream attachmentData(String file) throws IOException {
        InputStream data = new BufferedInputStream(new FileInputStream("src/test/resources/" + file));
        return wrapInDigestStream(data);
    }

    private static DigestInputStream wrapInDigestStream(InputStream data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return new DigestInputStream(data, digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Test is invalid, message digest algorithm not found.", ex);
        }
    }

    private static void assertEqualData(DigestInputStream expected, DigestInputStream actual) throws IOException {
        // First must read through all of the actual stream to be digested.
        try (OutputStream os = OutputStream.nullOutputStream()) {
            actual.transferTo(os);
        }

        assertArrayEquals("Downloaded attachment data doesn't match what was uploaded",
                expected.getMessageDigest().digest(),
                actual.getMessageDigest().digest());
    }

    private CloseableConnectionPool createConnectionPool() throws IOException {
        File hsqldbFile = attachTempDir.newFile();
        JDBCPool jdbc = new JDBCPool();
        jdbc.setUrl("jdbc:hsqldb:" + hsqldbFile.toURI() + ";shutdown=true;hsqldb.lob_file_scale=4;hsqldb.lob_compressed=true");
        jdbc.setUser("SA");
        jdbc.setPassword("SA");

        try (Connection c = jdbc.getConnection()) {
            DbInitializer.initDb(c);
        } catch (SQLException ex) {
            throw new AppServerException("Cannot initialize logs service: " + ex.getMessage(), ex);
        }
        return new CloseableConnectionPool(jdbc);
    }

    private class CloseableConnectionPool implements ConnectionPool, AutoCloseable {

        private final JDBCPool pool;

        public CloseableConnectionPool(JDBCPool jdbcPool) {
            pool = jdbcPool;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return pool.getConnection();
        }

        @Override
        public void close() throws SQLException {
            pool.close(1);
        }
    }

}
