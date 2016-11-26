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
package com.redsaz.meterrier.services;

import com.redsaz.meterrier.api.ImportService;
import com.redsaz.meterrier.api.LogsService;
import java.io.InputStream;
import java.util.List;
import com.redsaz.meterrier.api.model.ImportInfo;
import com.redsaz.meterrier.api.model.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does not directly store logs, but is responsible for ensuring that the logs
 * and metadata sent to and retrieved from the store are correctly formatted,
 * sized, and without malicious/errorific content.
 *
 * Default values for jtl files:
 * timestamp,elapsed,label,responseCode,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class ProcessorImportService implements ImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorImportService.class);
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    private final ImportService srv;
    private final LogsService logsSrv;

    public ProcessorImportService(ImportService importService, LogsService logsService) {
        srv = importService;
        logsSrv = logsService;
    }

    @Override
    public void delete(long id) {
        srv.delete(id);
    }

    @Override
    public ImportInfo get(long id) {
        return srv.get(id);
    }

    @Override
    public List<ImportInfo> list() {
        return srv.list();
    }

    @Override
    public ImportInfo upload(InputStream raw, ImportInfo source) {
        ImportInfo result = srv.upload(raw, source);
        EXEC.submit(new ImporterCallable(srv, logsSrv, result));
        return result;
    }

    @Override
    public ImportInfo update(ImportInfo source) {
        ImportInfo result = srv.update(source);
        EXEC.submit(new ImporterCallable(srv, logsSrv, result));
        return result;
    }

    private static class ImporterCallable implements Callable<ImportInfo> {

        private final ImportService is;
        private final LogsService ls;
        private final ImportInfo source;

        public ImporterCallable(ImportService isrv, LogsService lsrv, ImportInfo info) {
            is = isrv;
            ls = lsrv;
            source = info;
        }

        @Override
        public ImportInfo call() throws Exception {
            try {
                LOGGER.info("Processing import {}...", source);
                BufferedReader br = new BufferedReader(new FileReader(source.getImportedFilename()));
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    LOGGER.info(line);
                }
                br.close();
                LOGGER.info("...Finished reading import. Creating Log entry...");
                Log log = ls.create(new Log(0, null, source.getImportedFilename(), source.getUploadedUtcMillis(), ""));
                LOGGER.info("...Finished creating Log entry {}. Deleting import entry {}...", log.getId(), source.getId());
                is.delete(source.getId());
                LOGGER.info("...Finished deleting import entry {}.", source.getId());
                LOGGER.info("...Finished processing import {}.", source);
                return source; // This should return something else, probably.
            } catch (Exception ex) {
                LOGGER.error("Unable to process import " + source.getId(), ex);
                throw ex;
            }
        }

    }
}
