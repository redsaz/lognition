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
package com.redsaz.lognition.services;

import com.redsaz.lognition.store.ConnectionPool;
import java.sql.Connection;
import java.sql.SQLException;
import org.hsqldb.jdbc.JDBCPool;

/**
 * Wrapper arround the JDBCPool. It is already a pool, might as well use it.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class JdbcPoolConnectionPool implements ConnectionPool {

    private final JDBCPool pool;

    public JdbcPoolConnectionPool(JDBCPool jdbcPool) {
        pool = jdbcPool;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return pool.getConnection();
    }
}
