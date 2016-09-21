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

import com.redsaz.meterrier.services.SanitizedLogsService;
import com.redsaz.meterrier.services.SanitizedNotesService;
import com.redsaz.meterrier.api.LogsService;
import com.redsaz.meterrier.api.NotesService;
import com.redsaz.meterrier.store.HsqlJdbc;
import com.redsaz.meterrier.store.HsqlLogsService;
import com.redsaz.meterrier.store.HsqlNotesService;
import com.redsaz.meterrier.view.Sanitizer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import org.hsqldb.jdbc.JDBCPool;

/**
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class ServiceProducers {

    private static final JDBCPool POOL = HsqlJdbc.initPool();

    @Produces
    @ApplicationScoped
    @Sanitizer
    public LogsService createSanitizedLogsService() {
        return new SanitizedLogsService(new HsqlLogsService(POOL));
    }

    @Produces
    @ApplicationScoped
    @Sanitizer
    public NotesService createSanitizedNotesService() {
        return new SanitizedNotesService(new HsqlNotesService(POOL));
    }
}
