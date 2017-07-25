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
import com.redsaz.meterrier.api.NotesService;
import com.redsaz.meterrier.store.HsqlImportService;
import com.redsaz.meterrier.store.HsqlJdbc;
import com.redsaz.meterrier.store.HsqlLogsService;
import com.redsaz.meterrier.store.HsqlNotesService;
import com.redsaz.meterrier.view.Processor;
import com.redsaz.meterrier.view.Sanitizer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import org.hsqldb.jdbc.JDBCPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class ServiceProducers {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProducers.class);
    private static final JDBCPool POOL = HsqlJdbc.initPool();
    private static final LogsService SANITIZER_LOGS_SERVICE = new SanitizerLogsService(new HsqlLogsService(POOL));
    private static final ImportService SANITIZER_IMPORT_SERVICE = new SanitizerImportService(new HsqlImportService(POOL));
    private static final ProcessorImportService PROCESSOR_IMPORT_SERVICE = new ProcessorImportService(
            SANITIZER_IMPORT_SERVICE, SANITIZER_LOGS_SERVICE, "./meterrier-data/logs");
    private static final NotesService SANITIZER_NOTES_SERVICE = new SanitizedNotesService(new HsqlNotesService(POOL));

    @Produces
    @ApplicationScoped
    @Sanitizer
    public LogsService createSanitizerLogsService() {
        return SANITIZER_LOGS_SERVICE;
    }

    @Produces
    @ApplicationScoped
    @Processor
    public ImportService createProcessorImportService() {
        return PROCESSOR_IMPORT_SERVICE;
    }

    @Produces
    @ApplicationScoped
    @Sanitizer
    public NotesService createSanitizedNotesService() {
        return SANITIZER_NOTES_SERVICE;
    }

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        SANITIZER_LOGS_SERVICE.get(-1L); // Grab any non-existing item from the service
        LOGGER.info("Started Meterrier.");
    }

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        LOGGER.info("Shutting down Meterrier.");
        PROCESSOR_IMPORT_SERVICE.shutdown();
    }

}
