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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private final ImportService srv;
    private final LogsService logsSrv;
    private final Converter converter;
    private final String convertedDir;
    private final Importer importer;
    private final Thread importerThread;

    public static void main(String[] args) throws Exception {
        final JDBCPool pool = HsqlJdbc.initPool();
        final LogsService saniLogSrv = new SanitizerLogsService(new HsqlLogsService(pool));
        final String convertedDir = "jtls/target/converted";
        final long now = System.currentTimeMillis();
        final Converter conv = new CsvJtlToAvroConverter();

        Importer imp = new Importer(saniLogSrv, conv, convertedDir);
        Thread impThread = new Thread(imp, "LogImporter-" + System.identityHashCode(imp));
        impThread.start();

        ImportInfo ii = new ImportInfo(now, "jtls/target/real-without-header.jtl", "real-" + now, "csv", now, "Good");
        imp.addJob(ii);

        imp.shutdown();
    }

    public ProcessorImportService(ImportService importService, LogsService logsService,
            Converter dataConverter, String convertedDirectory) {
        srv = importService;
        logsSrv = logsService;
        converter = dataConverter;
        convertedDir = convertedDirectory;
        importer = new Importer(logsSrv, converter, convertedDir);
        importerThread = new Thread(importer, "LogImporter-" + System.identityHashCode(importer));
        init();
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
        importer.addJob(result);
        return result;
    }

    @Override
    public ImportInfo update(ImportInfo source) {
        ImportInfo result = srv.update(source);
        importer.addJob(result);
        return result;
    }

    public void shutdown() {
        importer.shutdown();
    }

    private void init() {
        importerThread.start();
    }

    private static class Importer implements Runnable {

        private final LogsService logsSrv;
        private final Converter conv;
        private final String convertedDir;
        private final BlockingQueue<ImportInfo> awaitingImport = new LinkedBlockingQueue<>();
        private final AtomicBoolean shutdown = new AtomicBoolean();

        public Importer(LogsService logsService, Converter converter, String convertedDirectory) {
            logsSrv = logsService;
            conv = converter;
            convertedDir = convertedDirectory;
        }

        public void addJob(ImportInfo info) {
            awaitingImport.add(info);
        }

        @Override
        public void run() {
            while (!Thread.interrupted() && !shutdown.get()) {
                try {
                    LOGGER.info("Waiting for import...");
                    ImportInfo source = awaitingImport.take();
                    LOGGER.info("...importing...");
                    File avro = new File(convertedDir, String.format("%d.avro", source.getId()));
                    String hash = conv.convert(new File(source.getImportedFilename()), avro);
                    LOGGER.info("...SHA-256: {}...", hash);

                    Log sourceLog = new Log(source.getId(), source.getTitle(), source.getTitle(), source.getUploadedUtcMillis(), "");
                    Log resultLog = logsSrv.create(sourceLog);
                    LOGGER.info("...created log {}.", resultLog);
                } catch (InterruptedException ex) {
                    LOGGER.info("This is fine. We want this to happen.");
                    Thread.currentThread().interrupt();
                }
            }
        }

        /**
         * Signals the instance that once it has finished work on the current item, it is to stop.
         * If there are any additionl items waiting in the queue, they will not be processed.
         */
        public void shutdown() {
            shutdown.set(true);
        }
    }
}
