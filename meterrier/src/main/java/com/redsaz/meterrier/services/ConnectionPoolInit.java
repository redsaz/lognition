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

import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.redsaz.meterrier.store.ConnectionPool;
import com.redsaz.meterrier.store.DbInitializer;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import org.hsqldb.jdbc.JDBCPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for HSQL.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class ConnectionPoolInit {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionPoolInit.class);

    // Don't allow creation of utility class.
    private ConnectionPoolInit() {
    }

    /**
     * Since several services all share the same DB, there should not need to be multiple JDBCPools,
     * only one. Calling this once and then passing it into each service to create should be done.
     *
     * @return the JDBCPool to use for all services using the HSQLDB.
     */
    public static ConnectionPool initPool() {
        LOGGER.info("Initing DB...");
        File deciDir = new File("./meterrier-data");
        if (!deciDir.exists() && !deciDir.mkdirs()) {
            throw new RuntimeException("Could not create " + deciDir);
        }
        File deciDb = new File(deciDir, "meterrierdb");
        JDBCPool jdbc = new JDBCPool();
        jdbc.setUrl("jdbc:hsqldb:" + deciDb.toURI());
        jdbc.setUser("SA");
        jdbc.setPassword("SA");

        try (Connection c = jdbc.getConnection()) {
            DbInitializer.initDb(c);
        } catch (SQLException ex) {
            throw new AppServerException("Cannot initialize logs service: " + ex.getMessage(), ex);
        }
        LOGGER.info("...Finish Initing DB.");
        return new JdbcPoolConnectionPool(jdbc);
    }

// Inits a ConnectionPool for an SQLite-backed store.
//    public static ConnectionPool initPool() {
//        LOGGER.info("Initing DB...");
//        File deciDir = new File("./meterrier-data");
//        if (!deciDir.exists() && !deciDir.mkdirs()) {
//            throw new RuntimeException("Could not create " + deciDir);
//        }
//        File deciDb = new File(deciDir, "meterrier-sqlite");
//        try {
//            Connection c = DriverManager.getConnection("jdbc:sqlite:" + deciDb.toURI());
//            DbInitializer.initDb(c);
//
//            LOGGER.info("...Finish Initing DB.");
//            return new SingletonConnectionPool(c);
//        } catch (SQLException ex) {
//            throw new AppServerException("Cannot initialize logs service: " + ex.getMessage(), ex);
//        }
//    }
}
