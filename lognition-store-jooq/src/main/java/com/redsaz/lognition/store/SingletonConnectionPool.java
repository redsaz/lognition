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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

/**
 * For databases which only allow one connection (such as SQLite), this connection pool will let
 * only one connection be used at a time.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class SingletonConnectionPool implements ConnectionPool {

    private final BlockingQueue<Connection> queue;
    private static final Connection CLOSED_CONNECTION = new ClosedConnection();

    public SingletonConnectionPool(Connection connection) {
        queue = new ArrayBlockingQueue<>(1);
        queue.add(connection);
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return new SingletonConnection(queue.take());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted before acquiring connection.");
        }
    }

    private class SingletonConnection implements Connection {

        private Connection c;

        public SingletonConnection(Connection connection) {
            c = connection;
        }

        @Override
        public void close() throws SQLException {
            // Don't close, but instead add it back into the queue, and swap in a closed connection
            // instead, incase somebody tries to do a use-after-close.
            queue.add(c);
            c = CLOSED_CONNECTION;
        }

        @Override
        public Statement createStatement() throws SQLException {
            return c.createStatement();
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return c.prepareStatement(sql);
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return c.prepareCall(sql);
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return c.nativeSQL(sql);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            c.setAutoCommit(autoCommit);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return c.getAutoCommit();
        }

        @Override
        public void commit() throws SQLException {
            c.commit();
        }

        @Override
        public void rollback() throws SQLException {
            c.rollback();
        }

        @Override
        public boolean isClosed() throws SQLException {
            return c.isClosed();
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return c.getMetaData();
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            c.setReadOnly(readOnly);
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return c.isReadOnly();
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            c.setCatalog(catalog);
        }

        @Override
        public String getCatalog() throws SQLException {
            return c.getCatalog();
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            c.setTransactionIsolation(level);
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return c.getTransactionIsolation();
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return c.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            c.clearWarnings();
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return c.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return c.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return c.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return c.getTypeMap();
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            c.setTypeMap(map);
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            c.setHoldability(holdability);
        }

        @Override
        public int getHoldability() throws SQLException {
            return c.getHoldability();
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return c.setSavepoint();
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return c.setSavepoint(name);
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
            c.rollback(savepoint);
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            c.releaseSavepoint(savepoint);
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return c.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return c.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return c.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return c.prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return c.prepareStatement(sql, columnIndexes);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return c.prepareStatement(sql, columnNames);
        }

        @Override
        public Clob createClob() throws SQLException {
            return c.createClob();
        }

        @Override
        public Blob createBlob() throws SQLException {
            return c.createBlob();
        }

        @Override
        public NClob createNClob() throws SQLException {
            return c.createNClob();
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return c.createSQLXML();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return c.isValid(timeout);
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            c.setClientInfo(name, value);
        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            c.setClientInfo(properties);
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return c.getClientInfo(name);
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return c.getClientInfo();
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return c.createArrayOf(typeName, elements);
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return c.createStruct(typeName, attributes);
        }

        @Override
        public void setSchema(String schema) throws SQLException {
            c.setSchema(schema);
        }

        @Override
        public String getSchema() throws SQLException {
            return c.getSchema();
        }

        @Override
        public void abort(Executor executor) throws SQLException {
            c.abort(executor);
        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
            c.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return c.getNetworkTimeout();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return c.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return c.isWrapperFor(iface);
        }
    }

    private static final class ClosedConnection implements Connection {

        @Override
        public void close() throws SQLException {
        }

        @Override
        public boolean isClosed() throws SQLException {
            return true;
        }

        @Override
        public Statement createStatement() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void commit() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void rollback() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public String getCatalog() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void clearWarnings() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public int getHoldability() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public Clob createClob() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public Blob createBlob() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public NClob createNClob() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void setSchema(String schema) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public String getSchema() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void abort(Executor executor) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("Connection is closed");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            throw new SQLException("Connection is closed");
        }

    }
}
