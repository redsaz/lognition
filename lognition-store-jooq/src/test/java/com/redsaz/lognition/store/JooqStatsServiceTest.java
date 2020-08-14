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
package com.redsaz.lognition.store;

import com.redsaz.lognition.api.exceptions.AppServerException;
import com.redsaz.lognition.api.model.CodeCounts;
import com.redsaz.lognition.api.model.Log;
import com.redsaz.lognition.api.model.Log.Status;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import org.hsqldb.jdbc.JDBCPool;
import org.jooq.SQLDialect;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Redsaz <redsaz@gmail.com>
 */
public class JooqStatsServiceTest {

    @Rule
    public TemporaryFolder connectionDir = new TemporaryFolder();

    private static final CodeCounts CODE_COUNTS = new CodeCounts.Builder(0L)
            .increment("200").commitBin().build();

    @Test
    public void testCreateOrUpdateCodeCounts() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool()) {
            // Given a log with a label,
            JooqStatsService unit = new JooqStatsService(cp, SQLDialect.HSQLDB);
            JooqLogsService logSvc = new JooqLogsService(cp, SQLDialect.HSQLDB,
                    connectionDir.newFolder().toString());
            Log log = new Log(1, Status.COMPLETE, "test", "Test", "test.jtl", "notes");
            log = logSvc.create(log);
            unit.createSampleLabels(log.getId(), Collections.singletonList("overall"));

            // When code counts are stored,
            unit.createOrUpdateCodeCounts(log.getId(), 0, CODE_COUNTS);

            // Then, uh, nothing bad happens, I hope. Whoops no getters!
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOrUpdateCodeCounts_badLogId() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool()) {
            JooqStatsService unit = new JooqStatsService(cp, SQLDialect.HSQLDB);

            // Given a bad log ID (an ID of 0 or below),
            long badLogId = 0;

            // When attempting to create code counts,
            unit.createOrUpdateCodeCounts(badLogId, 0, CODE_COUNTS);

            // Then an illegal argument exception is thrown. (Checked by test harness)
        }
    }

    @Test(expected = NullPointerException.class)
    public void testCreateOrUpdateCodeCounts_nullCounts() throws IOException, SQLException {
        try (CloseableConnectionPool cp = createConnectionPool()) {
            JooqStatsService unit = new JooqStatsService(cp, SQLDialect.HSQLDB);

            // Given a null CodeCounts,
            // When attempting to create code counts,
            unit.createOrUpdateCodeCounts(1, 0, null);

            // Then a null pointer exception is thrown. (Checked by test harness)
        }
    }

    private CloseableConnectionPool createConnectionPool() throws IOException {
        File hsqldbFile = connectionDir.newFile();
        JDBCPool jdbc = new JDBCPool();
        jdbc.setUrl("jdbc:hsqldb:" + hsqldbFile.toURI() + ";shutdown=true;hsqldb.lob_file_scale=4;hsqldb.lob_compressed=true");
        jdbc.setUser("SA");
        jdbc.setPassword("SA");

        try (Connection c = jdbc.getConnection()) {
            DbInitializer.initDb(c);
        } catch (SQLException ex) {
            throw new AppServerException("Cannot initialize logs service: " + ex.getMessage(), ex);
        }
        return new CloseableConnectionPool(jdbc);
    }

    private class CloseableConnectionPool implements ConnectionPool, AutoCloseable {

        private final JDBCPool pool;

        public CloseableConnectionPool(JDBCPool jdbcPool) {
            pool = jdbcPool;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return pool.getConnection();
        }

        @Override
        public void close() throws SQLException {
            pool.close(1);
        }
    }
}
