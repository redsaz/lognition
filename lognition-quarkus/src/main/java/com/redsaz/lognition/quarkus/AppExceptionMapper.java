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

import com.redsaz.lognition.api.exceptions.AppClientException;
import com.redsaz.lognition.api.exceptions.AppException;
import com.redsaz.lognition.view.Templater;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/** @author Redsaz <redsaz@gmail.com> */
@Provider
public class AppExceptionMapper implements ExceptionMapper<AppException> {

  @Context HttpHeaders headers;

  private Templater cfg;

  public AppExceptionMapper() {}

  @Inject
  public AppExceptionMapper(Templater templater) {
    cfg = templater;
  }

  @Override
  public Response toResponse(AppException e) {
    if (headers.getAcceptableMediaTypes().contains(MediaType.TEXT_HTML_TYPE)) {
      return createHtmlResponse(e);
    }
    Response.ResponseBuilder resp =
        Response.status(500).entity("").type(MediaType.APPLICATION_JSON);
    if (e instanceof AppClientException) {
      resp.status(400);
    }
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
    Response.ResponseBuilder resp = Response.status(500);
    if (e instanceof AppClientException) {
      resp.status(400);
      root.put(
          "action",
          "Please <a href=\"javascript:history.back()\">go back</a>, check for any potential mistakes, and try again.");
    } else {
      root.put(
          "action",
          "Please <a href=\"javascript:history.back()\">go back</a> and try again. If the problem persists, contact the application team.");
    }
    String body = cfg.buildFromTemplate(root, "page.ftl");
    resp.entity(body).type(MediaType.TEXT_HTML_TYPE);
    return resp.build();
  }
}
