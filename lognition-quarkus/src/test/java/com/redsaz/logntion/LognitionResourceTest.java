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
import static org.mockito.Mockito.when;

import com.redsaz.lognition.api.LogsService;
import com.redsaz.lognition.api.model.Log;
import com.redsaz.lognition.view.Sanitizer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class LognitionResourceTest {

  @Sanitizer @InjectMock LogsService logs;

  @Test
  public void testHome() {
    when(logs.list())
        .thenReturn(
            Arrays.asList(
                new Log(
                    1, Log.Status.COMPLETE, "test", "Test Name", "test.hsqldb", "Test notes.")));
    given()
        .when()
        .get("/logs")
        .then()
        .statusCode(200)
        .body(containsString("Test Name"))
        .body(containsString("Test notes."));
  }
}
