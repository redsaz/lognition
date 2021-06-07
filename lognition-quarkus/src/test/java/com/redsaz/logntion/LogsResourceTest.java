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

import com.redsaz.lognition.api.LognitionMediaType;
import com.redsaz.lognition.api.LogsService;
import com.redsaz.lognition.api.model.Log;
import com.redsaz.lognition.view.Sanitizer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import static io.restassured.RestAssured.given;
import java.util.Arrays;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class LogsResourceTest {

    @Sanitizer
    @InjectMock
    LogsService logs;

    @Test
    public void testListLogsBrief() {
        when(logs.list()).thenReturn(Arrays.asList(new Log(1L, Log.Status.COMPLETE, "test", "Test Name", "test.hsqldb", "Test notes.")));
        given()
                .when().accept(LognitionMediaType.LOGBRIEF_V1_JSON).get("/logs")
                .then()
                .statusCode(200)
                .body(containsString("Test Name"))
                .body(containsString("Test notes."))
                .log().everything();
    }

    @Test
    public void testListLogsBrief_LabelSelector() {
        when(logs.listIdsBySelector(any())).thenReturn(Arrays.asList(1L));
        when(logs.get(anyLong())).thenReturn(
                new Log(1L, Log.Status.COMPLETE, "test", "Test Name", "test.hsqldb", "Test notes.")
        );

        given()
                .when().accept(LognitionMediaType.LOGBRIEF_V1_JSON).get("/logs?labelSelector=a")
                .then()
                .statusCode(200)
                .body(containsString("test"))
                .body(containsString("Test Name"))
                // Filename should not be exposed, internal detail.
                .body(Matchers.not("filename"));

        verify(logs).listIdsBySelector(any());
        verify(logs).get(1L);
    }

    @Test
    public void testListLogsBrief_BadLabelSelector() {
        // Test that when the user provides a bad selector that the right error message is returned
        // and not a stack trace.
        when(logs.listIdsBySelector(any())).thenReturn(Arrays.asList(1L));
        when(logs.get(anyLong())).thenReturn(
                new Log(1L, Log.Status.COMPLETE, "test", "Test Name", "test.hsqldb", "Test notes.")
        );

        given()
                .when().accept(LognitionMediaType.LOGBRIEF_V1_JSON).get("/logs?labelSelector=in")
                .then()
                .statusCode(400)
                .body(equalTo("{\"error\":\"LabelSelectorSyntaxException\",\"message\":\"Label Selector syntax error. Expected ! or label key but got in instead.\"}"));
    }
}
