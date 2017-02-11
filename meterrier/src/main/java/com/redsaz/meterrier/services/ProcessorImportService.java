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
import com.redsaz.meterrier.importer.AvroToCsvJtlConverter;
import com.redsaz.meterrier.importer.Converter;
import com.redsaz.meterrier.importer.CsvJtlToAvroConverter;
import com.redsaz.meterrier.importer.CsvJtlToCsvJtlConverter;
import java.io.File;
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
    private final Converter converter;

    public static void main(String[] args) throws Exception {
        ImportInfo ii = new ImportInfo(0, "jtls/real-with-header.jtl", "title", "csv", 1234567890000L, "Good");
        Converter imp = new CsvJtlToAvroConverter();
        ImporterCallable ic = new ImporterCallable(imp, ii);
        ic.call();
    }

    public ProcessorImportService(ImportService importService, LogsService logsService) {
        srv = importService;
        logsSrv = logsService;
        converter = new CsvJtlToAvroConverter();
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
        EXEC.submit(new ImporterCallable(converter, result));
        return result;
    }

    @Override
    public ImportInfo update(ImportInfo source) {
        ImportInfo result = srv.update(source);
        EXEC.submit(new ImporterCallable(converter, result));
        return result;
    }

    private static class ImporterCallable implements Callable<ImportInfo> {

        private final Converter conv;
        private final ImportInfo source;

        public ImporterCallable(Converter converter, ImportInfo info) {
            conv = converter;
            source = info;
        }

        @Override
        public ImportInfo call() throws Exception {
            LOGGER.info("Make a 1:1 baseline jtl which has all the information kept in our avro form.");
            File baselineDest = new File("jtls/real-columntrimmed.csv");
            Converter c2c = new CsvJtlToCsvJtlConverter();
            c2c.convert(new File(source.getImportedFilename()), baselineDest);

            LOGGER.info("Now do the actual conversion.");
            File dest = new File("jtls/real.avro");
            conv.convert(new File(source.getImportedFilename()), dest);

            LOGGER.info("Now export it back to a jtl for comparison against the baseline.");
            Converter a2j = new AvroToCsvJtlConverter();
            a2j.convert(dest, new File("jtls/should-equal-real-columntrimmed.jtl"));
            return source;
        }

    }
}
