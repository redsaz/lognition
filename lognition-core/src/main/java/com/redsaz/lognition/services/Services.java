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
package com.redsaz.lognition.services;

import com.redsaz.lognition.api.AttachmentsService;
import com.redsaz.lognition.api.ImportService;
import com.redsaz.lognition.api.LogsService;
import com.redsaz.lognition.api.ReviewsService;
import com.redsaz.lognition.api.StatsService;
import com.redsaz.lognition.api.exceptions.AppServerException;
import com.redsaz.lognition.store.ConnectionPool;
import com.redsaz.lognition.store.HsqldbConnectionPool;
import com.redsaz.lognition.store.JooqAttachmentsService;
import com.redsaz.lognition.store.JooqImportService;
import com.redsaz.lognition.store.JooqLogsService;
import com.redsaz.lognition.store.JooqReviewsService;
import com.redsaz.lognition.store.JooqStatsService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import org.jooq.SQLDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central location for Lognition services.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class Services implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(Services.class);

  private static final String LOGS_DIR = "./lognition-data/logs";
  private static final String ATTACHMENTS_DIR = "./lognition-data/attachmentsDir";

  private final ConnectionPool connectionPool;
  private final AttachmentsService attachmentsService;
  private final LogsService logsService;
  private final ReviewsService reviewsService;
  private final ImportService importService;
  private final StatsService statsService;
  private final ProcessorImportService processorImportService;

  public Services(String embeddedDbPath, boolean autoinit) {
    try {
      LOG.info("Loading DB at embeddedDbPath={}", embeddedDbPath);
      this.connectionPool = hsqldbPool(Paths.get(embeddedDbPath), autoinit);
    } catch (SQLException ex) {
      throw new AppServerException("Could not open database.", ex);
    }

    this.attachmentsService =
        new SanitizerAttachmentsService(
            new JooqAttachmentsService(this.connectionPool, SQLDialect.HSQLDB, ATTACHMENTS_DIR));
    this.logsService =
        new SanitizerLogsService(
            new JooqLogsService(
                this.connectionPool, SQLDialect.HSQLDB, LOGS_DIR, this.attachmentsService));
    this.reviewsService =
        new SanitizerReviewsService(
            new JooqReviewsService(
                this.connectionPool, SQLDialect.HSQLDB, this.attachmentsService));
    this.importService =
        new SanitizerImportService(new JooqImportService(this.connectionPool, SQLDialect.HSQLDB));
    this.statsService = new JooqStatsService(this.connectionPool, SQLDialect.HSQLDB);
    this.processorImportService =
        new ProcessorImportService(importService, logsService, statsService, LOGS_DIR);
    LOG.info("Started Lognition Services.");
  }

  public LogsService sanitizerLogsService() {
    return logsService;
  }

  public ReviewsService sanitizerReviewsService() {
    return reviewsService;
  }

  public ImportService processorImportService() {
    return processorImportService;
  }

  public StatsService statsService() {
    return statsService;
  }

  @Override
  public void close() throws Exception {
    LOG.info("Closing Lognition services.");
    processorImportService.shutdown();
    try {
      connectionPool.close();
    } catch (SQLException ex) {
      throw new AppServerException("Error closing database connections.", ex);
    }
  }

  private static ConnectionPool hsqldbPool(Path hsqldbFilepath, boolean autoinit)
      throws SQLException {
    try {
      Files.createDirectories(hsqldbFilepath.getParent());
    } catch (IOException ex) {
      throw new SQLException("Could not create directories for " + hsqldbFilepath);
    }
    if (autoinit) {
      return HsqldbConnectionPool.initAndOpen(hsqldbFilepath);
    }
    return HsqldbConnectionPool.open(hsqldbFilepath);
  }
}
