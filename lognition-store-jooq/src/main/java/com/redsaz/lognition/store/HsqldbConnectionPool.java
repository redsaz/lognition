/*
 * Copyright 2024 Redsaz <redsaz@gmail.com>.
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

import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.ShardingKeyBuilder;
import java.util.ServiceConfigurationError;
import org.hsqldb.jdbc.JDBCPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around the JDBCPool. It is already a pool, might as well use it.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class HsqldbConnectionPool implements ConnectionPool {
  private static final Logger LOG = LoggerFactory.getLogger(HsqldbConnectionPool.class);

  private final JDBCPool pool;

  public HsqldbConnectionPool(JDBCPool jdbcPool) {
    pool = jdbcPool;
  }

  /**
   * Open a database connection only. Does not first check if the database schema needs
   * created/migrated.
   *
   * <p>Note: This may need to be used when using Quarkus or frameworks that may use a custom
   * classloader during tests which can interfere with Flyway loading its plugins correctly. If
   * "java.util.ServiceConfigurationError: org.flywaydb.core.extensibility.Plugin:
   * org.flywaydb.core.internal.database.WHATEVER not a subtype" is seen during testing, then use
   * this open method instead of initAndOpen.
   *
   * @param hsqldbFilepath The path to place the DB at.
   * @return An Hsqldb connection pool
   * @throws SQLException if the connection pool could not be created.
   */
  public static HsqldbConnectionPool open(Path hsqldbFilepath) throws SQLException {
    String url =
        "jdbc:hsqldb:"
            + hsqldbFilepath.toUri()
            + ";shutdown=true;hsqldb.lob_file_scale=4;hsqldb.lob_compressed=true";
    String user = "SA";
    String password = "SA";

    // Now start up the DB for realsies
    JDBCPool jdbc = new JDBCPool();
    jdbc.setUrl(url);
    jdbc.setUser(user);
    jdbc.setPassword(password);

    return new HsqldbConnectionPool(jdbc);
  }

  public static HsqldbConnectionPool initAndOpen(Path hsqldbFilepath) throws SQLException {
    String url =
        "jdbc:hsqldb:"
            + hsqldbFilepath.toUri()
            + ";shutdown=true;hsqldb.lob_file_scale=4;hsqldb.lob_compressed=true";
    String user = "SA";
    String password = "SA";

    try {
      DbInitializer.init(hsqldbFilepath, url, user, password);
    } catch (ServiceConfigurationError ex) {
      // This can happen when running integration tests in lognition-app, with quarkus's custom
      // classloader. For now, log it and move on.
      // "Caused by: java.util.ServiceConfigurationError:
      // org.flywaydb.core.extensibility.Plugin:
      // org.flywaydb.core.internal.database.h2.H2DatabaseType not a subtype"
      // TODO: make it not do that.
      LOG.error("Unable to run DB auto-initialization. Skipping.", ex);
    }

    JDBCPool jdbc = new JDBCPool();
    jdbc.setUrl(url);
    jdbc.setUser(user);
    jdbc.setPassword(password);

    return new HsqldbConnectionPool(jdbc);
  }

  @Override
  public Connection getConnection() throws SQLException {
    return pool.getConnection();
  }

  @Override
  public void close() throws SQLException {
    pool.close(0);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return pool.getConnection(username, password);
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return pool.getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    pool.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    pool.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return pool.getLoginTimeout();
  }

  @Override
  public ConnectionBuilder createConnectionBuilder() throws SQLException {
    return pool.createConnectionBuilder();
  }

  @Override
  public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return pool.getParentLogger();
  }

  @Override
  public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
    return pool.createShardingKeyBuilder();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return pool.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return pool.isWrapperFor(iface);
  }
}
