/*
 * Copyright 2021 Redsaz <redsaz@gmail.com>.
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
package com.redsaz.lognition.app.services;

import com.redsaz.lognition.api.ImportService;
import com.redsaz.lognition.api.LogsService;
import com.redsaz.lognition.api.ReviewsService;
import com.redsaz.lognition.api.StatsService;
import com.redsaz.lognition.services.Services;
import com.redsaz.lognition.view.Processor;
import com.redsaz.lognition.view.Sanitizer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Redsaz <redsaz@gmail.com>
 */
@Singleton
public class ServiceProducers implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceProducers.class);
  private final Services services;

  public ServiceProducers(
      @ConfigProperty(name = "lognition.data.embeddeddb.location") String embeddedDbPath,
      @ConfigProperty(name = "lognition.data.embeddeddb.autoinit") boolean autoinit) {
    services = new Services(embeddedDbPath, autoinit);
  }

  @Produces
  @ApplicationScoped
  @Sanitizer
  public LogsService createSanitizerLogsService() {
    return services.sanitizerLogsService();
  }

  @Produces
  @ApplicationScoped
  @Sanitizer
  public ReviewsService createSanitizerReviewsService() {
    return services.sanitizerReviewsService();
  }

  @Produces
  @ApplicationScoped
  @Processor
  public ImportService createProcessorImportService() {
    return services.processorImportService();
  }

  @Produces
  @ApplicationScoped
  public StatsService createStatsService() {
    return services.statsService();
  }

  public void init() {
    LOG.info("Started up Lognition.");
  }

  @Override
  public void close() throws Exception {
    LOG.info("Shutting down Lognition.");
    services.close();
  }

  public void onStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
    init();
  }

  public void onStop(@Observes @Destroyed(ApplicationScoped.class) Object destroy)
      throws Exception {
    close();
  }
}
