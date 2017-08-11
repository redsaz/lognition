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
import com.redsaz.meterrier.store.ConnectionPool;
import com.redsaz.meterrier.store.JooqImportService;
import com.redsaz.meterrier.store.JooqLogsService;
import com.redsaz.meterrier.view.Processor;
import com.redsaz.meterrier.view.Sanitizer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import org.jooq.SQLDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class ServiceProducers {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProducers.class);
    private static final String LOGS_DIR = "./meterrier-data/logs";
    private static final ConnectionPool POOL = ConnectionPoolInit.initPool();
    private static final LogsService SANITIZER_LOGS_SERVICE = new SanitizerLogsService(new JooqLogsService(POOL, SQLDialect.HSQLDB, LOGS_DIR));
    private static final ImportService SANITIZER_IMPORT_SERVICE = new SanitizerImportService(new JooqImportService(POOL, SQLDialect.HSQLDB));
    private static final ProcessorImportService PROCESSOR_IMPORT_SERVICE = new ProcessorImportService(
            SANITIZER_IMPORT_SERVICE, SANITIZER_LOGS_SERVICE, LOGS_DIR);

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

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        SANITIZER_LOGS_SERVICE.get(-1L); // Grab any non-existing item from the service
        LOGGER.info("Started Meterrier.");
    }

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        LOGGER.info("Shutting down Meterrier.");
        PROCESSOR_IMPORT_SERVICE.shutdown();
    }

}
