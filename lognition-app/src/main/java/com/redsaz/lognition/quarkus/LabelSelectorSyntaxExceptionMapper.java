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
package com.redsaz.lognition.quarkus;

import com.redsaz.lognition.api.labelselector.LabelSelectorSyntaxException;
import com.redsaz.lognition.view.Templater;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Redsaz <redsaz@gmail.com>
 */
@Provider
public class LabelSelectorSyntaxExceptionMapper
    implements ExceptionMapper<LabelSelectorSyntaxException> {

  @Context HttpHeaders headers;

  private Templater cfg;

  public LabelSelectorSyntaxExceptionMapper() {}

  @Inject
  public LabelSelectorSyntaxExceptionMapper(Templater templater) {
    cfg = templater;
  }

  @Override
  public Response toResponse(LabelSelectorSyntaxException e) {
    if (headers.getAcceptableMediaTypes().contains(MediaType.TEXT_HTML_TYPE)) {
      return createHtmlResponse(e);
    }
    Response.ResponseBuilder resp =
        Response.status(400)
            .entity(new ErrorMessage(e.getClass().getSimpleName(), e.getMessage()))
            .type(MediaType.APPLICATION_JSON);
    return resp.build();
  }

  private Response createHtmlResponse(Throwable e) {
    Map<String, Object> root = new HashMap<>();
    String base = "";
    String dist = base + "/dist";
    root.put("base", base);
    root.put("dist", dist);
    root.put("title", "Bad Request");
    root.put("message", e.getMessage());
    root.put("content", "error.ftl");
    Response.ResponseBuilder resp = Response.status(400);
    root.put(
        "action",
        "Please <a href=\"javascript:history.back()\">go back</a> and try again. If the problem persists, contact the application team.");
    String body = cfg.buildFromTemplate(root, "page.ftl");
    resp.entity(body).type(MediaType.TEXT_HTML_TYPE);
    return resp.build();
  }
}
