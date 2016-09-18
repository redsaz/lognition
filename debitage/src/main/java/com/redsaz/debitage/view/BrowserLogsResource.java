/*
 * Copyright 2016 Redsaz <redsaz@gmail.com>.
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
package com.redsaz.debitage.view;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import com.redsaz.debitage.api.LogsService;
import com.redsaz.debitage.api.model.Log;
import com.redsaz.debitage.api.model.LogBrief;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;

/**
 * An endpoint for accessing measurements/logs. The REST endpoints and browser
 * endpoints are identical; look at docs/endpoints.md for why.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
@Path("/logs")
public class BrowserLogsResource {

    private static final Logger LOGGER = Logger.getLogger(BrowserLogsResource.class.getName());

    private LogsService logsSrv;
    private Templater cfg;

    public BrowserLogsResource() {
    }

    @Inject
    public BrowserLogsResource(LogsService logsService, Templater config) {
        logsSrv = logsService;
        cfg = config;
    }

    /**
     * Presents a web page of {@link LogBrief}s.
     *
     * @param httpRequest The request for the page.
     * @return Briefs, by URI and title.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response listLogBriefs(@Context HttpServletRequest httpRequest) {
        String base = httpRequest.getContextPath();
        String dist = base + "/dist";
        List<Log> briefs = logsSrv.getLogs();

        Map<String, Object> root = new HashMap<>();
        root.put("briefs", briefs);
        root.put("base", base);
        root.put("dist", dist);
        root.put("title", "Logs");
        root.put("content", "log-list.ftl");
        return Response.ok(cfg.buildFromTemplate(root, "page.ftl")).build();
    }

    /**
     * Presents a web page for viewing a specific log brief.
     *
     * @param httpRequest The request for the page.
     * @param id The id of the brief.
     * @return Brief view page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("{id}")
    public Response getLogBrief(@Context HttpServletRequest httpRequest, @PathParam("id") long id) {
        String base = httpRequest.getContextPath();
        String dist = base + "/dist";
        Log brief = logsSrv.getLog(id);
        if (brief == null) {
            throw new NotFoundException("Could not find note id=" + id);
        }
        Map<String, Object> root = new HashMap<>();
        root.put("brief", brief);
        root.put("base", base);
        root.put("dist", dist);
        root.put("title", brief.getId());
//        root.put("title", brief.getTitle());
        root.put("content", "log-view.ftl");
        return Response.ok(cfg.buildFromTemplate(root, "page.ftl")).build();
    }

    @POST
    @Consumes("multipart/form-data")
    @Produces({MediaType.TEXT_HTML})
    public Response finishCreateLog(MultipartInput input) {
        LOGGER.log(Level.INFO, "Creating log...");
        try {
            String title = null;
            String notes = null;
            Log content = null;
            String filename = null;
            long updateMillis = System.currentTimeMillis();
            ContentDispositionSubParts subParts = new ContentDispositionSubParts();
            for (InputPart part : input.getParts()) {
                subParts.clear();
                String partContentDisposition = part.getHeaders().getFirst("Content-Disposition");
                parseContentDispositionHeader(partContentDisposition, subParts);
                switch (subParts.getName()) {
                    case "content":
                        try (InputStream contentStream = part.getBody(InputStream.class, null)) {
                            LOGGER.log(Level.INFO, "Retrieving filename...");
                            filename = subParts.getFilename();
                            LOGGER.log(Level.INFO, "Uploading content from {0}...", filename);
                            content = logsSrv.createLog(contentStream);
                            LOGGER.log(Level.INFO, "Uploaded  content from {0}.", filename);
                            LOGGER.log(Level.INFO, "Created Log {0}.", content.getId());
                            break;
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, "BAD STUFF:" + ex.getMessage(), ex);
                            Response resp = Response.serverError().entity(ex).build();
                            return resp;
                        }
                    case "title":
                        try {
                            title = part.getBodyAsString();
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, "BAD STUFF:" + ex.getMessage(), ex);
                            Response resp = Response.serverError().entity(ex).build();
                            return resp;
                        }
                        break;
                    case "notes":
                        try {
                            notes = part.getBodyAsString();
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, "BAD STUFF:" + ex.getMessage(), ex);
                            Response resp = Response.serverError().entity(ex).build();
                            return resp;
                        }
                        break;
                    default:
                        // Skip it, we don't use it.
                        break;
                }
            }
//            if (content != null) {
//                LogBrief source = new LogBrief(0L, null, title, notes, filename, updateMillis, content.getId());
//                logsSrv.createBrief(source);
//            }
            Response resp = Response.seeOther(URI.create("logs")).build();
            LOGGER.log(Level.INFO, "Finished creating log {0}", content);
            return resp;
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "BAD STUFF:" + ex.getMessage(), ex);
            throw ex;
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    /**
     * Presents a web page for uploading log.
     *
     * @param httpRequest The request for the page.
     * @return log upload page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("create")
    public Response createLog(@Context HttpServletRequest httpRequest) {
        String base = httpRequest.getContextPath();
        String dist = base + "/dist";
        Map<String, Object> root = new HashMap<>();
        root.put("base", base);
        root.put("dist", dist);
        root.put("title", "Create Log");
        root.put("content", "log-create.ftl");
        return Response.ok(cfg.buildFromTemplate(root, "page.ftl")).build();
    }

    @POST
    @Path("delete")
    public Response deleteLog(@FormParam("id") long id) {
        logsSrv.deleteLog(id);
        Response resp = Response.seeOther(URI.create("/logs")).build();
        return resp;
    }

    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
    private static void parseContentDispositionHeader(String headerValue, NameValueListener listener) {
        // Examples:
        // form-data; name="content"; filename="file-asdf.txt"
        // form-data; name="content"; filename="file-with-quote\"-asdf.txt"
        // form-data; name="content"; filename="file-with-backslash\-asdf.txt"
        // form-data; name="content"; filename="file-with-backslash-and-quote\\"-asdf.txt"
        // Basically, if a backslash is encountered, check the next char. If
        // the next char is a quote, ignore the backslash, otherwise keep it.
        listener.beginParse();
        int cursor = headerValue.indexOf(";");
        if (cursor < 0) {
            listener.endParse();
            return;
        }
        ++cursor; // Skip the first entry because it is usually "form-data;"
        String[] nameValPair = new String[]{null, null};
        for (int length = headerValue.length(); cursor < length && cursor >= 0;) {
            cursor = getNameValue(headerValue, cursor, nameValPair);
            // -1 means that, though there was more string available,
            // there were no name-value pairs.
            if (cursor >= 0) {
                listener.nameValue(nameValPair[0], nameValPair[1]);
                // Now find the next semi-colon (or end-of-message)
            }
        }
        listener.endParse();
    }

    private static int getNameValue(String source, int cursor, String[] outNameValPair) {
        outNameValPair[0] = outNameValPair[1] = null;
        // First, skip to the first non-semicolon or space.
        for (int length = source.length(); cursor < length; ++cursor) {
            char curr = source.charAt(cursor);
            if (curr != ';' & curr != ' ') {
                break;
            }
        }
        // Now find the name part.
        int equalsPos = source.indexOf("=", cursor);
        // If no key-value pairs were found, return -1.
        if (equalsPos < 0) {
            return -1;
        }
        outNameValPair[0] = source.substring(cursor, equalsPos).trim();
        cursor = ++equalsPos;
        // Now, find the value part.
        StringBuilder value = new StringBuilder();
        boolean insideQuotes = false;
        for (int length = source.length(); cursor < length; ++cursor) {
            char curr = source.charAt(cursor);
            if (curr == '"') {
                // If we already were inside the quotes, then it means we
                // reached the second quotes mark, so advance cursor to the pos
                // after the quote and leave.
                if (insideQuotes) {
                    ++cursor;
                    break;
                }
                // otherwite, this is the first quote, and we should start
                // making the value string.
                insideQuotes = true;
            } else if (insideQuotes) {
                // Handle special case: if backslash appears, and then next
                // char is a quote, then append the quote, not the backslash.
                if (curr == '\\') {
                    if (cursor < length && source.charAt(cursor + 1) == '"') {
                        ++cursor;
                        curr = '"';
                    }
                }
                value.append(curr);
            }
        }
        outNameValPair[1] = value.toString();
        return cursor;
    }

    private static interface NameValueListener {

        void beginParse();

        void nameValue(String name, String value);

        void endParse();
    }

    private static class ContentDispositionSubParts implements NameValueListener {

        private String name;
        private String filename;

        public String getName() {
            return name;
        }

        public String getFilename() {
            return filename;
        }

        public void clear() {
            name = null;
            filename = null;
        }

        @Override
        public void beginParse() {
            clear();
        }

        @Override
        public void nameValue(String inName, String inValue) {
            switch (inName) {
                case "name":
                    name = inValue;
                    break;
                case "filename":
                    filename = inValue;
                    break;
                default:
                    // Skip it, we don't use it.
                    break;
            }
        }

        @Override
        public void endParse() {
            // Good.
        }

    }
}
