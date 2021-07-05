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
package com.redsaz.logntion;

import com.redsaz.lognition.api.ReviewsService;
import com.redsaz.lognition.api.model.Attachment;
import com.redsaz.lognition.api.model.Review;
import com.redsaz.lognition.view.Sanitizer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class BrowserReviewsResourceTest {

    @Sanitizer
    @InjectMock
    ReviewsService reviews;

    @Test
    public void testListReviews() {
        when(reviews.list()).thenReturn(Arrays.asList(new Review(1L, "test-name", "Test Name", "Test desc.", 1620789842000L, 1620789842000L, "test body")));
        given()
                .when().get("/reviews")
                .then()
                .statusCode(200)
                .body(containsString("Test Name"))
                .body(containsString("Test desc."));
    }

    @Test
    public void testUploadAttachments() {
        when(reviews.putAttachment(anyLong(), any(Attachment.class), any(InputStream.class)))
                .thenReturn(new Attachment(1, "review-1", "path.jpg", "Image", "It is an image", "image/jpeg", System.currentTimeMillis()));

        given()
                .when()
                // Do not follow redirects, we're only testing this endpoint
                .config(config().redirect(config().getRedirectConfig().followRedirects(false)))
                .contentType("multipart/form-data")
                .accept("application/json")
                .multiPart("path", "path.jpg")
                .multiPart("name", "Image")
                .multiPart("description", "It is an image")
                .multiPart("file", new File("src/test/resources/image.jpg"), "image/jpeg")
                .post("reviews/1/attachments")
                .then()
                .statusCode(303);

        ArgumentCaptor<Attachment> actual = ArgumentCaptor.forClass(Attachment.class);
        verify(reviews).putAttachment(eq(1L), actual.capture(), any(InputStream.class));

        // Yoink the uploadedUtcMillis from the actual source attachment to create the expected
        // source attachment that should be passed to the service, but everything else should be
        // compared.
        Attachment actualSource = actual.getValue();
        Attachment expectedSource = new Attachment(0L, "", "path.jpg", "Image", "It is an image", "image/jpeg", actualSource.getUploadedUtcMillis());

        assertEquals(expectedSource, actualSource, "The attachment details to be stored are incorrect.");
    }

    @Test
    public void testUploadAttachments_noOptionalParams() throws IOException {
        when(reviews.putAttachment(anyLong(), any(Attachment.class), any(InputStream.class)))
                .thenAnswer(imock -> {
                    long reviewId = imock.getArgument(0);
                    Attachment source = imock.getArgument(1);
                    return new Attachment(reviewId, "review-" + reviewId, source.getPath(), source.getName(), source.getDescription(), source.getMimeType(), System.currentTimeMillis());
                });
        when(reviews.getAttachmentData(anyLong(), any(String.class)))
                .thenReturn(new BufferedInputStream(
                        new FileInputStream("src/test/resources/image.jpg"))
                );

        // Really, the only part necessary is the file. Everything else is optional.
        given()
                .when()
                // Do not follow redirects, we're only testing this endpoint
                .config(config().redirect(config().getRedirectConfig().followRedirects(false)))
                .contentType("multipart/form-data")
                .accept("application/json")
                .multiPart("file", new File("src/test/resources/image.jpg"))
                .post("reviews/1/attachments")
                .then()
                .statusCode(303);

        ArgumentCaptor<Attachment> actual = ArgumentCaptor.forClass(Attachment.class);
        verify(reviews).putAttachment(eq(1L), actual.capture(), any(InputStream.class));

        // Make sure all the source attachment info is correct (except for the upload time). Because
        // no optional data was used, make sure that:
        // - path comes from the filename
        // - name comes from the path (which in this case come from the filename)
        // - description is an empty string
        // - curl will usually use application/octet-stream if mimetype isn't specified.
        Attachment actualSource = actual.getValue();
        Attachment expectedSource = new Attachment(0L, "", "image.jpg", "image.jpg", "", "application/octet-stream", actualSource.getUploadedUtcMillis());

        assertEquals(expectedSource, actualSource, "The creation attachment details to be stored are incorrect.");

        // Because the uploaded attachment was application/octet-stream, the method will analyze the
        // uploaded attachment to determine the file type, and update the attachment details.
        // This time around, the updated details should match the attachment details it received
        // from the initial upload, and only the mimetype changed.
        verify(reviews).getAttachmentData(1L, "image.jpg");

        ArgumentCaptor<Attachment> actual2 = ArgumentCaptor.forClass(Attachment.class);
        verify(reviews).updateAttachment(eq(1L), actual2.capture());

        actualSource = actual2.getValue();
        expectedSource = new Attachment(1L, "review-1", "image.jpg", "image.jpg", "", "image/jpeg", actualSource.getUploadedUtcMillis());

        assertEquals(expectedSource, actualSource, "The updated attachment details are incorrect.");
    }

    @Test
    public void testUploadAttachments_noFileParam() throws IOException {

        // Really, the only part necessary is the file. Everything else is optional.
        given()
                .when()
                // Do not follow redirects, we're only testing this endpoint
                .config(config().redirect(config().getRedirectConfig().followRedirects(false)))
                .contentType("multipart/form-data")
                .accept("application/json")
                .multiPart("name", "Image")
                .post("reviews/1/attachments")
                .then()
                .statusCode(400)
                .log()
                .body()
                .body(containsString("Missing \\\"file\\\""))
                .body(containsString("Required multiPart \\\"file\\\" is missing."));

        // If no file was provided, then do not attempt to upload the attachment.
        verifyNoInteractions(reviews);
    }
}
