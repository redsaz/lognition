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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.redsaz.lognition.api.LogsService;
import com.redsaz.lognition.api.StatsService;
import com.redsaz.lognition.api.model.CodeCounts;
import com.redsaz.lognition.api.model.Histogram;
import com.redsaz.lognition.api.model.Label;
import com.redsaz.lognition.api.model.Log;
import com.redsaz.lognition.api.model.Percentiles;
import com.redsaz.lognition.api.model.Stats;
import com.redsaz.lognition.api.model.Timeseries;
import com.redsaz.lognition.view.Sanitizer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class BrowserLogsResourceTest {

  @Sanitizer @InjectMock LogsService logs;

  @InjectMock StatsService stats;

  @Test
  public void testListLogsBrief() {
    Log actual = new Log(1, Log.Status.COMPLETE, "test", "Test Name", "test.hsqldb", "Test notes.");
    when(logs.list()).thenReturn(Arrays.asList(actual));
    given()
        .when()
        .get("/logs")
        .then()
        .statusCode(200)
        .body(containsString("Test Name"))
        .body(containsString("Test notes."));
  }

  @Test
  public void testGetLogBriefById() {
    simpleLogTest("/logs/1");
  }

  @Test
  public void testGetLogBriefByIdWithName() {
    simpleLogTest("/logs/1/somewords");
  }

  private void simpleLogTest(String path) {
    // Simulates a log with two responses, made across two minutes.

    Log actual = new Log(1, Log.Status.COMPLETE, "test", "Test Name", "test.hsqldb", "Test notes.");
    when(logs.get(anyLong())).thenReturn(actual);

    List<String> actualSampleLabels = List.of("overall", "GET");
    when(stats.getSampleLabels(anyLong())).thenReturn(actualSampleLabels);

    Map<Long, CodeCounts> actualAggregateCodeCounts = new HashMap<>();
    CodeCounts overallCodeCounts =
        new CodeCounts.Builder(0L).increment("200").increment("200").commitBin().build();
    actualAggregateCodeCounts.put(0L, overallCodeCounts);
    CodeCounts getCodeCounts =
        new CodeCounts.Builder(0L).increment("200").increment("200").commitBin().build();
    actualAggregateCodeCounts.put(1L, getCodeCounts);
    when(stats.getCodeCountsForLog(anyLong(), eq(0L))).thenReturn(actualAggregateCodeCounts);

    Map<Long, CodeCounts> actualTimeseriesCodeCounts = new HashMap<>();
    CodeCounts overallTsCodeCounts =
        new CodeCounts.Builder(60_000L)
            .increment("200")
            .commitBin()
            .increment("200")
            .commitBin()
            .build();
    actualTimeseriesCodeCounts.put(0L, overallTsCodeCounts);
    CodeCounts getTsCodeCounts =
        new CodeCounts.Builder(0L)
            .increment("200")
            .commitBin()
            .increment("200")
            .commitBin()
            .build();
    actualTimeseriesCodeCounts.put(1L, getTsCodeCounts);
    when(stats.getCodeCountsForLog(anyLong(), eq(60_000L))).thenReturn(actualTimeseriesCodeCounts);

    // Timeseries
    Timeseries actualTimeseries =
        new Timeseries(
            60_000L,
            List.of(
                new Stats(0L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 1L, 2048L, 0L),
                new Stats(
                    0L, 200L, 200L, 200L, 200L, 200L, 200L, 200L, 200L, 200L, 1L, 2048L, 0L)));
    when(stats.getTimeseries(anyLong(), anyLong())).thenReturn(actualTimeseries);

    // Aggregate
    Stats actualAggregate =
        new Stats(0L, 100L, 100L, 200L, 200L, 200L, 200L, 200L, 200L, 150L, 2L, 4096L, 0L);
    when(stats.getAggregate(anyLong(), anyLong())).thenReturn(actualAggregate);

    // Histogram
    Histogram actualHistogram =
        new Histogram(List.of(0L, 1L, 0L, 1L), List.of(50L, 100L, 150L, 200L));
    when(stats.getHistogram(anyLong(), anyLong())).thenReturn(actualHistogram);

    // Percentiles
    Percentiles actualPercentiles =
        new Percentiles(
            List.of(1L, 2L, 2L, 2L),
            List.of(100L, 200L, 200L, 200L),
            List.of(0.25, 0.50, 0.75, 0.95));
    when(stats.getPercentiles(anyLong(), anyLong())).thenReturn(actualPercentiles);

    // Log Labels
    List<Label> actualLabels = List.of(new Label("example-log", "test"));
    when(logs.getLabels(anyLong())).thenReturn(actualLabels);

    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            new BaseMatcher<String>() {
              @Override
              public boolean matches(Object actual) {
                System.out.println(actual);
                return true;
              }

              @Override
              public void describeTo(Description description) {}
            })
        .body(containsString(">overall<"))
        .body(containsString(">GET<"));
  }

  @Test
  public void testGetLogBriefById_noCodeCounts() {
    withoutCodeCountsLogTest("/logs/1");
  }

  @Test
  public void testGetLogBriefByIdWithName_noCodeCounts() {
    withoutCodeCountsLogTest("/logs/1/somewords");
  }

  private void withoutCodeCountsLogTest(String path) {
    // Simulates a log with two responses, made across two minutes.
    // No code counts are provided, which is how logs used to be created. The page should still
    // be displayed.

    Log actual = new Log(1, Log.Status.COMPLETE, "test", "Test Name", "test.hsqldb", "Test notes.");
    when(logs.get(anyLong())).thenReturn(actual);

    List<String> actualSampleLabels = List.of("overall", "GET");
    when(stats.getSampleLabels(anyLong())).thenReturn(actualSampleLabels);

    // No code counts
    Map<Long, CodeCounts> actualAggregateCodeCounts = Collections.emptyMap();
    when(stats.getCodeCountsForLog(anyLong(), anyLong())).thenReturn(actualAggregateCodeCounts);

    // Timeseries
    Timeseries actualTimeseries =
        new Timeseries(
            60_000L,
            List.of(
                new Stats(0L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 1L, 2048L, 0L),
                new Stats(
                    0L, 200L, 200L, 200L, 200L, 200L, 200L, 200L, 200L, 200L, 1L, 2048L, 0L)));
    when(stats.getTimeseries(anyLong(), anyLong())).thenReturn(actualTimeseries);

    // Aggregate
    Stats actualAggregate =
        new Stats(0L, 100L, 100L, 200L, 200L, 200L, 200L, 200L, 200L, 150L, 2L, 4096L, 0L);
    when(stats.getAggregate(anyLong(), anyLong())).thenReturn(actualAggregate);

    // Histogram
    Histogram actualHistogram =
        new Histogram(List.of(0L, 1L, 0L, 1L), List.of(50L, 100L, 150L, 200L));
    when(stats.getHistogram(anyLong(), anyLong())).thenReturn(actualHistogram);

    // Percentiles
    Percentiles actualPercentiles =
        new Percentiles(
            List.of(1L, 2L, 2L, 2L),
            List.of(100L, 200L, 200L, 200L),
            List.of(0.25, 0.50, 0.75, 0.95));
    when(stats.getPercentiles(anyLong(), anyLong())).thenReturn(actualPercentiles);

    // Log Labels
    List<Label> actualLabels = List.of(new Label("example-log", "test"));
    when(logs.getLabels(anyLong())).thenReturn(actualLabels);

    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("Response statistics were not collected for this log."))
        .body(containsString("Response timeseries were not collected for this log."));
  }
}
