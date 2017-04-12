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
import com.redsaz.meterrier.convert.Converter;
import com.redsaz.meterrier.convert.CsvJtlToAvroConverter;
import com.redsaz.meterrier.store.HsqlJdbc;
import com.redsaz.meterrier.store.HsqlLogsService;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.hsqldb.jdbc.JDBCPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does not directly store logs, but is responsible for ensuring that the logs and metadata sent to
 * and retrieved from the store are correctly formatted, sized, and without malicious/errorific
 * content.
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
    private final String convertedDir;

    public static void main(String[] args) throws Exception {
        JDBCPool pool = HsqlJdbc.initPool();
        LogsService saniLogSrv = new SanitizerLogsService(new HsqlLogsService(pool));
        String convertedDir = "jtls/target/converted";
        long now = System.currentTimeMillis();
        ImportInfo ii = new ImportInfo(now, "jtls/target/real-without-header.jtl", "real-" + now, "csv", now, "Good");
        Converter imp = new CsvJtlToAvroConverter();
        ImporterCallable ic = new ImporterCallable(saniLogSrv, imp, convertedDir, ii);
        ic.call();
    }

    public ProcessorImportService(ImportService importService, LogsService logsService,
            Converter dataConverter, String convertedDirectory) {
        srv = importService;
        logsSrv = logsService;
        converter = dataConverter;
        convertedDir = convertedDirectory;
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
        EXEC.submit(new ImporterCallable(logsSrv, converter, convertedDir, result));
        return result;
    }

    @Override
    public ImportInfo update(ImportInfo source) {
        ImportInfo result = srv.update(source);
        EXEC.submit(new ImporterCallable(logsSrv, converter, convertedDir, result));
        return result;
    }

    private static class ImporterCallable implements Callable<File> {

        private final LogsService logsSrv;
        private final Converter conv;
        private final String convertedDir;
        private final ImportInfo source;

        public ImporterCallable(LogsService logsService, Converter converter, String convertedDirectory, ImportInfo info) {
            logsSrv = logsService;
            conv = converter;
            convertedDir = convertedDirectory;
            source = info;
        }

        @Override
        public File call() throws Exception {
            File converted = convert();

            Log sourceLog = new Log(source.getId(), source.getTitle(), source.getTitle(), source.getUploadedUtcMillis(), "");
            Log resultLog = logsSrv.create(sourceLog);
            LOGGER.info("Created log {}", resultLog);

            return converted;
        }

        private File convert() {
            File avro = new File(convertedDir, String.format("%d.avro", source.getId()));
            String hash = conv.convert(new File(source.getImportedFilename()), avro);
            LOGGER.info("SHA-256: {}", hash);

            return avro;
        }

    }
}
