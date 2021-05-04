/*
 * Copyright 2020 Redsaz <redsaz@gmail.com>.
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
package com.redsaz.lognition.services;

import com.redsaz.lognition.api.ImportService;
import com.redsaz.lognition.api.LogsService;
import com.redsaz.lognition.api.StatsService;
import com.redsaz.lognition.api.model.ImportInfo;
import com.redsaz.lognition.api.model.Log;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProcessorImportServiceTest {

    @Rule
    public TemporaryFolder importDir = new TemporaryFolder();

    @Test
    public void testUpload() throws IOException {
        // Given a CSV-based JTL file,
        ImportService impSvc = mock(ImportService.class);
        LogsService logSvc = mock(LogsService.class);
        StatsService statsSvc = mock(StatsService.class);
        String importDirStr = importDir.getRoot().toString();

        ProcessorImportService unit = new ProcessorImportService(impSvc, logSvc, statsSvc, importDirStr);

        long uploadedUtc = 1595126270000L;
        long importedUtc = 1595126271000L;

        Log log = new Log(1L, Log.Status.AWAITING_UPLOAD, "test", "Test", "testtest.csv", "notes");

        doNothing().when(logSvc).updateStatus(anyLong(), anyObject());
        InputStream is = Files.newInputStream(Paths.get("jtls/target/real-small-sorted.jtl"));
        ImportInfo imported = new ImportInfo(1L, "jtls/target/real-small-sorted.jtl", importedUtc);
        when(impSvc.upload(same(is), same(log), eq(importDirStr), eq(uploadedUtc))).thenReturn(imported);

        // This is how to wait for the eager stats calc to complete since it is async.
        long lastLabelId = 20L;
        CountDownLatch lastStatsComplete = new CountDownLatch(1);
        doAnswer((invocation) -> {
            lastStatsComplete.countDown();
            return null;
        }).when(statsSvc).createOrUpdatePercentiles(eq(log.getId()), eq(lastLabelId), anyObject());

        // When the file is imported,
        ImportInfo ii = unit.upload(is, log, importDirStr, uploadedUtc);
        await(lastStatsComplete);

        // Then an avro file should be in the imported dir with the log id,
        assertTrue(Files.exists(Paths.get(importDirStr, log.getId() + ".avro")));
        // and the stats should be eagerly calculated.
        verify(statsSvc).createSampleLabels(eq(log.getId()), anyObject());

        for (long i = 0; i <= lastLabelId; ++i) {
            verify(statsSvc, times(2)).createOrUpdateCodeCounts(eq(log.getId()), eq(i), anyObject());
            verify(statsSvc).createOrUpdateTimeseries(eq(log.getId()), eq(i), anyObject());
            verify(statsSvc).createOrUpdateAggregate(eq(log.getId()), eq(i), anyObject());
            verify(statsSvc).createOrUpdateHistogram(eq(log.getId()), eq(i), anyObject());
            verify(statsSvc).createOrUpdatePercentiles(eq(log.getId()), eq(i), anyObject());
        }

        // Uploading is status whilst receiving bytes
        verify(logSvc).updateStatus(eq(log.getId()), eq(Log.Status.UPLOADING));
        // Importing is status whilst converting the stored JTL into Avro
        verify(logSvc).updateStatus(eq(log.getId()), eq(Log.Status.IMPORTING));
        // Complete is the final status, but it still has to eagerly calculate stats.
        verify(logSvc).updateStatus(eq(log.getId()), eq(Log.Status.COMPLETE));
        // Data-to-import should be deleted after successfully being imported.
        verify(impSvc).delete(eq(ii.getId()));

    }

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(10, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            fail("Test interrupted.");
        }
        return false;
    }
}
