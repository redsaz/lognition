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

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redsaz.lognition.api.LognitionMediaType;
import com.redsaz.lognition.api.LogsService;
import com.redsaz.lognition.api.ReviewsService;
import com.redsaz.lognition.api.model.Log;
import com.redsaz.lognition.api.model.Review;
import com.redsaz.lognition.view.Sanitizer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ReviewsResourceTest {

  @Sanitizer @InjectMock LogsService logs;

  @Sanitizer @InjectMock ReviewsService reviews;

  @Test
  public void testListReviews() {
    when(reviews.list())
        .thenReturn(
            Arrays.asList(
                new Review(
                    1L, "test1", "Test1", "description", 1231231234000L, 1231231234000L, "test"),
                new Review(
                    2L, "test2", "Test2", "description", 1231231234000L, 1231231234000L, "test")));
    given()
        .when()
        .accept(LognitionMediaType.REVIEW_V1_JSON)
        .get("/reviews")
        .then()
        .statusCode(200)
        .body(containsString("test1"))
        .body(containsString("Test2"));
  }

  @Test
  public void testListReviews_none() {
    when(reviews.list()).thenReturn(Collections.emptyList());
    given()
        .when()
        .accept(LognitionMediaType.REVIEW_V1_JSON)
        .get("/reviews")
        .then()
        .body(equalTo("[]"))
        .statusCode(200);
  }

  @Test
  public void testGetReview() {
    when(reviews.get(eq(1L)))
        .thenReturn(
            new Review(
                1L, "test1", "Test1", "description", 1231231234000L, 1231231234000L, "test"));
    given()
        .when()
        .accept(LognitionMediaType.REVIEW_V1_JSON)
        .get("/reviews/1")
        .then()
        .statusCode(200)
        .body(containsString("test1"));
  }

  @Test
  public void testCreateReview() {
    when(reviews.create(any()))
        .thenReturn(
            new Review(
                1L, "test1", "Test1", "description", 1231231234000L, 1231231234000L, "test"));
    given()
        .when()
        .accept(LognitionMediaType.REVIEW_V1_JSON)
        .contentType(LognitionMediaType.REVIEW_V1_JSON)
        .body("{\"name\": \"test1\", \"description\": \"testdesc\", \"body\": \"test\"}")
        .post("/reviews")
        .then()
        .statusCode(201)
        .body(containsString("test1"))
        .header("Location", endsWith("/reviews/1"));
  }

  @Test
  public void testDeleteReview() {
    given().when().delete("/reviews/1").then().statusCode(204);
    verify(reviews).delete(1L);
  }

  @Test
  public void testListLogs() {
    when(reviews.getReviewLogs(anyLong()))
        .thenReturn(
            Arrays.asList(
                new Log(
                    1L, Log.Status.COMPLETE, "test", "Test Name", "test.hsqldb", "Test notes.")));
    given()
        .when()
        .accept(LognitionMediaType.LOGBRIEF_V1_JSON)
        .get("/reviews/1/logs")
        .then()
        .statusCode(200)
        .body(containsString("\"id\":1"))
        .body(containsString("test"))
        .body(containsString("Test Name"));
  }

  @Test
  public void testListLogs_empty() {
    when(reviews.getReviewLogs(anyLong())).thenReturn(Collections.emptyList());
    given()
        .when()
        .accept(LognitionMediaType.LOGBRIEF_V1_JSON)
        .get("/reviews/1/logs")
        .then()
        .statusCode(200)
        .body(containsString("[]"));
  }
}
