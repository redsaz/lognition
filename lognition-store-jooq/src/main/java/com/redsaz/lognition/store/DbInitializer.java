/*
 * Copyright 2017 Redsaz <redsaz@gmail.com>.
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
package com.redsaz.lognition.store;

import com.redsaz.lognition.api.exceptions.AppServerException;
import java.sql.Connection;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class DbInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbInitializer.class);

    // Don't allow util classes to be created.
    private DbInitializer() {
    }

    public static void initDb(Connection c) {
        try {
            LOGGER.info("Initing DB...");
            // If the database doesn't exist, create it according to the spec.
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
            Liquibase liquibase = new Liquibase("lognition-db.yaml", new ClassLoaderResourceAccessor(), database);
            // Update the database if it does exist, if needed.
            liquibase.update((String) null);
        } catch (LiquibaseException ex) {
            throw new AppServerException("Cannot initialize database: " + ex.getMessage(), ex);
        }
        LOGGER.info("...Finish Initing DB.");
    }

}
