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
import com.redsaz.lognition.api.model.Review;
import com.redsaz.lognition.view.Sanitizer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import static io.restassured.RestAssured.given;
import java.util.Arrays;
import static org.hamcrest.CoreMatchers.containsString;
import org.junit.jupiter.api.Test;
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

}
