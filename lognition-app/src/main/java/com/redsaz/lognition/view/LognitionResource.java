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
package com.redsaz.lognition.view;

import com.redsaz.lognition.api.ImportService;
import com.redsaz.lognition.api.LogsService;
import com.redsaz.lognition.api.StatsService;
import com.redsaz.lognition.api.model.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Redsaz <redsaz@gmail.com>
 */
@Path("/")
public class LognitionResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(LognitionResource.class);

  private LogsService logsSrv;
  private ImportService importSrv;
  private StatsService statsSrv;
  private Templater cfg;

  private static final Parser CM_PARSER = Parser.builder().build();
  private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().escapeHtml(true).build();

  public LognitionResource() {}

  @Inject
  public LognitionResource(
      @Sanitizer LogsService logsService,
      @Processor ImportService importService,
      StatsService statsService,
      Templater config) {
    logsSrv = logsService;
    importSrv = importService;
    statsSrv = statsService;
    cfg = config;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public Response home() {
    String dist = "/dist";
    List<Log> logs = logsSrv.list();

    Map<String, Object> root = new HashMap<>();
    root.put("briefs", logs);
    root.put("base", "");
    root.put("dist", dist);
    root.put("title", "Lognition");
    root.put("content", "home.ftl");
    return Response.ok(cfg.buildFromTemplate(root, "base.ftl")).build();
  }
}
