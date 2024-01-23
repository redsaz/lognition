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

import com.redsaz.lognition.api.exceptions.AppServerException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.hsqldb.jdbc.JDBCDataSource;

/**
 * Initializes and migrates the database schema as necessary.
 *
 * @author redsaz
 */
public class DbInitializer {

  // do not allow utility class to be constructed
  private DbInitializer() {}

  public static void init(Path hsqldbFilepath, String url, String user, String password)
      throws SQLException {
    try {
      Files.createDirectories(hsqldbFilepath.getParent());
    } catch (IOException ex) {
      throw new SQLException("Could not create directories for " + hsqldbFilepath);
    }

    try {
      JDBCDataSource ds = new JDBCDataSource();
      ds.setUrl(url);
      ds.setUser(user);
      ds.setPassword(password);
      Flyway flyway = Flyway.configure().baselineOnMigrate(true).dataSource(ds).load();
      flyway.migrate();
    } catch (FlywayException ex) {
      throw new AppServerException("Cannot initialize database: " + ex.getMessage(), ex);
    }
  }
}
