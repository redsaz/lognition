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
package com.redsaz.lognition.api;

import com.redsaz.lognition.api.model.Attachment;
import com.redsaz.lognition.api.model.Log;
import com.redsaz.lognition.api.model.Review;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * Stores and accesses {@link Review}s.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public interface ReviewsService {

    Review create(Review source);

    Review get(long id);

    List<Review> list();

    Review update(Review source);

    void delete(long id);

    void setReviewLogs(long reviewId, Collection<Long> logIds);

    List<Log> getReviewLogs(long reviewId);

    /**
     * Adds a new attachment or replaces an existing attachment. If the reviewId+attachment.path
     * combo already exist, then the attachment will be replaced.
     *
     * @param reviewId the owner of the attachment
     * @param source details of the attachment
     * @param data the attachment contents
     * @return The resulting Attachment data
     */
    Attachment putAttachment(long reviewId, Attachment source, InputStream data);

    /**
     * Updates any details of the attachment (name, description, etc, but not path or data). To move
     * an attachment to a new path, use
     * {@link #moveAttachment(long, java.lang.String, java.lang.String)}.
     *
     * @param reviewId the owner of the attachment
     * @param source the updated details of the attachment, including a new attachment path
     * @return The resulting Attachment data
     */
    Attachment updateAttachment(long reviewId, Attachment source);

    /**
     * Moves an attachment from one path to another. If the target path is already used by another
     * attachment, then the other attachment will be deleted and replaced by the the source
     * attachment.
     *
     * @param reviewId the owner of the attachment
     * @param sourcePath the original attachment path
     * @param targetPath the destination attachment path
     * @return The attachment that was moved
     */
    Attachment moveAttachment(long reviewId, String sourcePath, String targetPath);

    /**
     * List all attachments for a single review.
     *
     * @param reviewId the owner of the attachments
     * @return a list of attachments for the review, or an empty list if none exist
     */
    List<Attachment> listAttachments(long reviewId);

    InputStream getAttachmentData(long reviewId, String attachmentPath);

    Attachment getAttachment(long reviewId, String attachmentPath);

    void deleteAttachment(long reviewId, String attachmentPath);
}
