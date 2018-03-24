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
package com.redsaz.meterrier.view;

import com.redsaz.meterrier.api.ImportService;
import com.redsaz.meterrier.api.LogsService;
import com.redsaz.meterrier.api.StatsService;
import com.redsaz.meterrier.api.exceptions.AppClientException;
import com.redsaz.meterrier.api.model.Histogram;
import com.redsaz.meterrier.api.model.ImportInfo;
import com.redsaz.meterrier.api.model.Log;
import com.redsaz.meterrier.api.model.LogBrief;
import com.redsaz.meterrier.api.model.Percentiles;
import com.redsaz.meterrier.api.model.Stats;
import com.redsaz.meterrier.api.model.Timeseries;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An endpoint for accessing measurements/logs. The REST endpoints and browser endpoints are
 * identical; look at docs/endpoints.md for why.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
@Path("/logs")
public class BrowserLogsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserLogsResource.class);

    private LogsService logsSrv;
    private ImportService importSrv;
    private StatsService statsSrv;
    private Templater cfg;

    private static final Parser CM_PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().escapeHtml(true).build();

    public BrowserLogsResource() {
    }

    @Inject
    public BrowserLogsResource(@Sanitizer LogsService logsService,
            @Processor ImportService importService, StatsService statsService, Templater config) {
        logsSrv = logsService;
        importSrv = importService;
        statsSrv = statsService;
        cfg = config;
    }

    /**
     * Presents a web page of {@link LogBrief}s.
     *
     * @param httpRequest The request for the page.
     * @return Web page of LogBriefs.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response listLogBriefs(@Context HttpServletRequest httpRequest) {
        String base = httpRequest.getContextPath();
        String dist = base + "/dist";
        List<Log> logs = logsSrv.list();

        Map<String, Object> root = new HashMap<>();
        root.put("briefs", logs);
        root.put("base", base);
        root.put("dist", dist);
        root.put("title", "Logs");
        root.put("content", "log-list.ftl");
        return Response.ok(cfg.buildFromTemplate(root, "page.ftl")).build();
    }

    /**
     * Presents a web page for viewing a specific log brief.
     *
     * @param req The request for the page.
     * @param logId The id of the brief.
     * @return Brief view page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("{id}")
    public Response getLogBriefById(@Context HttpServletRequest req, @PathParam("id") long logId) {
        return getLogBrief(req, logId, null);
    }

    /**
     * Presents a web page for viewing a specific log brief.
     *
     * @param req The request for the page.
     * @param logId The id of the brief.
     * @param urlName The urlName of the brief.
     * @return Brief view page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("{id}/{urlName}")
    public Response getLogBriefByIdWithName(@Context HttpServletRequest req,
            @PathParam("id") long logId, @PathParam("urlName") String urlName) {
        return getLogBrief(req, logId, urlName);
    }

    @POST
    @Consumes("multipart/form-data")
    @Produces({MediaType.TEXT_HTML})
    public Response finishImportLog(MultipartInput input) {
        LOGGER.info("Uploading log for import...");
        try {
            ImportInfo content = null;
            String name = null;
            String filename = null;
            String notes = null;
            long updateMillis = System.currentTimeMillis();
            ContentDispositionSubParts subParts = new ContentDispositionSubParts();
            for (InputPart part : input.getParts()) {
                subParts.clear();
                String partContentDisposition = part.getHeaders().getFirst("Content-Disposition");
                parseContentDispositionHeader(partContentDisposition, subParts);
                switch (subParts.getName()) {
                    case "content":
                        try (InputStream contentStream = part.getBody(InputStream.class, null)) {
                            LOGGER.info("Retrieving filename...");
                            filename = subParts.getFilename();
                            LOGGER.info("Uploading content from filename={}...", filename);

                            Log resultLog = null;
                            try {
                                Log sourceLog = new Log(0L, Log.Status.AWAITING_UPLOAD, null, name, null, notes);
                                resultLog = logsSrv.create(sourceLog);

                                content = importSrv.upload(contentStream, resultLog, filename, updateMillis);
                                LOGGER.info("Uploaded content from {}.", filename);
                                LOGGER.info("Created import_id={}.", content.getId());
                            } catch (AppClientException ex) {
                                if (resultLog != null) {
                                    logsSrv.delete(resultLog.getId());
                                }
                                throw ex;
                            }
                            break;
                        } catch (IOException ex) {
                            LOGGER.error("BAD STUFF:" + ex.getMessage(), ex);
                            Response resp = Response.serverError().entity(ex).build();
                            return resp;
                        }
                    case "name":
                        try {
                            name = part.getBodyAsString();
                        } catch (IOException ex) {
                            LOGGER.error("Error getting name.", ex);
                        }
                        break;
                    case "notes":
                        try {
                            notes = part.getBodyAsString();
                        } catch (IOException ex) {
                            LOGGER.error("Error getting notes.", ex);
                        }
                        break;
                    default: {
                        // Skip it, we don't use it.
                        LOGGER.info("Skipped part={}", subParts.getName());
                    }
                    break;
                }
            }
            Response resp = Response.seeOther(URI.create("logs")).build();
            LOGGER.info("Finished uploading log {} for import", content);
            return resp;
        } catch (RuntimeException ex) {
            LOGGER.error("BAD STUFF:" + ex.getMessage(), ex);
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
    @Path("import")
    public Response importLog(@Context HttpServletRequest httpRequest) {
        String base = httpRequest.getContextPath();
        String dist = base + "/dist";
        Map<String, Object> root = new HashMap<>();
        root.put("base", base);
        root.put("dist", dist);
        root.put("title", "Import Log");
        root.put("content", "log-import.ftl");
        return Response.ok(cfg.buildFromTemplate(root, "page.ftl")).build();
    }

    /**
     * Presents a web page for editing log.
     *
     * @param httpRequest The request for the page.
     * @return log edit page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("{id}/edit")
    public Response editLog(@Context HttpServletRequest httpRequest, @PathParam("id") long logId) {
        String base = httpRequest.getContextPath();
        String dist = base + "/dist";

        Map<String, Object> root = new HashMap<>();
        Log log = logsSrv.get(logId);
        if (log == null) {
            throw new NotFoundException("Could not find logId=" + logId);
        }
        LOGGER.info("Editing log={}", log);

        root.put("brief", log);
        root.put("base", base);
        root.put("dist", dist);
        root.put("title", "Edit Log");
        root.put("content", "log-edit.ftl");
        return Response.ok(cfg.buildFromTemplate(root, "page.ftl")).build();
    }

    @POST
    @Consumes("multipart/form-data")
    @Produces(MediaType.TEXT_HTML)
    @Path("{id}")
    public Response finishEditLog(MultipartInput input, @PathParam("id") long logId) {
        LOGGER.info("Submitting log {} edits...", logId);
        try {
            String name = null;
            String notes = null;
            ContentDispositionSubParts subParts = new ContentDispositionSubParts();
            for (InputPart part : input.getParts()) {
                subParts.clear();
                String partContentDisposition = part.getHeaders().getFirst("Content-Disposition");
                parseContentDispositionHeader(partContentDisposition, subParts);
                switch (subParts.getName()) {
                    case "name":
                        try {
                            name = part.getBodyAsString();
                        } catch (IOException ex) {
                            LOGGER.error("Error getting name.", ex);
                        }
                        break;
                    case "notes":
                        try {
                            notes = part.getBodyAsString();
                        } catch (IOException ex) {
                            LOGGER.error("Error getting notes.", ex);
                        }
                        break;
                    default: {
                        // Skip it, we don't use it.
                        LOGGER.info("Skipped part={}", subParts.getName());
                    }
                    break;
                }
            }
            Log sourceLog = new Log(logId, null, null, name, null, notes);
            logsSrv.update(sourceLog);

            Response resp = Response.seeOther(URI.create("logs/" + logId)).build();
            LOGGER.info("Finished updating log {}.", logId);
            return resp;
        } catch (RuntimeException ex) {
            LOGGER.error("BAD STUFF:" + ex.getMessage(), ex);
            throw ex;
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    @POST
    @Path("delete")
    public Response deleteLog(@FormParam("id") long id) {
        logsSrv.delete(id);
        Response resp = Response.seeOther(URI.create("/logs")).build();
        return resp;
    }

    /**
     * Presents a web page for viewing a specific log brief. The actual log displayed only really
     * depends on the log id, the URL name is optional. If the given URL name doesn't match the real
     * URL name of the log with the id, then a redirect will happen.
     *
     * @param req The request for the page.
     * @param logId The id of the brief.
     * @param uriName The optional log URI name. May be null.
     * @return Brief view page if the urlName matches the reall urlName for the log with the id.
     */
    private Response getLogBrief(HttpServletRequest req, long logId, String uriName) {
        String base = req.getContextPath();
        String dist = base + "/dist";
        Log log = logsSrv.get(logId);
        if (log == null) {
            throw new NotFoundException("Could not find logId=" + logId);
        } else if (!Objects.equals(uriName, log.getUriName())
                && log.getUriName() != null
                && !log.getUriName().isEmpty()) {
            LOGGER.debug("logId={} provided name was \"{}\" but expected \"{}\".", logId, uriName, log.getUriName());
            return Response.seeOther(URI.create("logs/" + logId + "/" + log.getUriName()))
                    .status(Response.Status.MOVED_PERMANENTLY)
                    .build();
        }

        List<String> labels = statsSrv.getSampleLabels(logId);
        List<String> graphs = new ArrayList<>(labels.size());
        List<Stats> aggregates = new ArrayList<>(labels.size());
        List<String> histogramGraphs = new ArrayList<>(labels.size());
        List<String> percentileGraphs = new ArrayList<>(labels.size());
        for (int i = 0; i < labels.size(); ++i) {
            String label = labels.get(i);

            Timeseries timeseries = statsSrv.getTimeseries(logId, i);
            String dygraph = createTimeseriesGraph(timeseries, label, i);
            graphs.add(dygraph);

            Stats aggregate = statsSrv.getAggregate(logId, i);
            aggregates.add(aggregate);

            Histogram histogram = statsSrv.getHistogram(logId, i);
            String histogramGraph = createHistogramGraph(histogram, label, i);
            histogramGraphs.add(histogramGraph);

            Percentiles percentile = statsSrv.getPercentiles(logId, i);
            String percentileGraph = createPercentileGraph(percentile, label, i);
            percentileGraphs.add(percentileGraph);
        }

        Map<String, Object> root = new HashMap<>();
        root.put("brief", log);
        root.put("notesHtml", commonMarkToHtml(log.getNotes()));
        root.put("sampleLabels", labels);
        root.put("graphs", graphs);
        root.put("aggregates", aggregates);
        root.put("histogramGraphs", histogramGraphs);
        root.put("percentileGraphs", percentileGraphs);
        root.put("base", base);
        root.put("dist", dist);
        root.put("title", log.getName());
        root.put("content", "log-view.ftl");
        return Response.ok(cfg.buildFromTemplate(root, "page.ftl")).build();
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

    private static String commonMarkToHtml(String commonMarkText) {
        Node document = CM_PARSER.parse(commonMarkText);
        return HTML_RENDERER.render(document);
    }

    private static String createTimeseriesGraph(Timeseries timeseries, String label, int index) {
        if (timeseries == null) {
            LOGGER.debug("Timeseries is empty.");
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("new Dygraph(document.getElementById(\"graphdiv").append(index).append("\"),\n");
        String csvRowTail = " +\n";
        sb.append("\"offsetMillis,p50,p25,p75\\n\"").append(csvRowTail);
        List<Stats> statsList = timeseries.getStatsList();
        for (Stats stats : statsList) {
            sb.append("\"")
                    .append(stats.getOffsetMillis())
                    .append(",")
                    .append(stats.getP25())
                    .append(";")
                    .append(stats.getP50())
                    .append(";")
                    .append(stats.getP75())
                    .append(",")
                    .append(stats.getP25())
                    .append(";")
                    .append(stats.getP25())
                    .append(";")
                    .append(stats.getP25())
                    .append(",")
                    .append(stats.getP75())
                    .append(";")
                    .append(stats.getP75())
                    .append(";")
                    .append(stats.getP75())
                    .append("\\n\"")
                    .append(csvRowTail);
        }
        if (!statsList.isEmpty()) {
            sb.setLength(sb.length() - csvRowTail.length());
        }
        sb.append(", {\n");
        sb.append("legend: 'always',\n");
        sb.append("title: '").append(label).append("',\n");
        sb.append("customBars: true,\n");
        sb.append("xlabel: 'Time',\n");
        sb.append("ylabel: 'Response Time (ms)',\n");
        sb.append("});");
        return sb.toString();
    }

    private static String createHistogramGraph(Histogram histogram, String label, int index) {
        if (histogram == null) {
            LOGGER.debug("Histogram is empty.");
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("new Dygraph(document.getElementById(\"histogramgraphdiv").append(index).append("\"),\n");
        String csvRowTail = " +\n";
        sb.append("\"maxInclusive,count\\n\"").append(csvRowTail);
        for (int i = 0; i < histogram.size(); ++i) {
            sb.append("\"")
                    .append(histogram.bucketMaxInclusive(i))
                    .append(",")
                    .append(histogram.bucketCount(i))
                    .append("\\n\"")
                    .append(csvRowTail);
        }
        if (histogram.size() != 0) {
            sb.setLength(sb.length() - csvRowTail.length());
        }
        sb.append(", {\n");
        sb.append("legend: 'always',\n");
        sb.append("title: '").append(label).append(" Histogram',\n");
        sb.append("xlabel: 'Response Time (ms)',\n");
        sb.append("ylabel: 'count',\n");
        sb.append("});");
        return sb.toString();
    }

    private static String createPercentileGraph(Percentiles percentile, String label, int index) {
        if (percentile == null) {
            LOGGER.debug("Percentile is empty.");
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("new Dygraph(document.getElementById(\"percentilegraphdiv").append(index).append("\"),\n");
        String csvRowTail = " +\n";
        sb.append("\"percentile,Response time (ms)\\n\"").append(csvRowTail);
        for (int i = 0; i < percentile.size(); ++i) {
            sb.append("\"")
                    .append(percentile.getPercentiles().get(i))
                    .append(",")
                    .append(percentile.getValues().get(i))
                    .append("\\n\"")
                    .append(csvRowTail);
        }
        if (percentile.size() != 0) {
            sb.setLength(sb.length() - csvRowTail.length());
        }
        sb.append(", {\n");
        sb.append("legend: 'always',\n");
        sb.append("title: '").append(label).append(" Percentiles',\n");
        sb.append("xlabel: 'Percentile',\n");
        sb.append("ylabel: 'Response Time (ms)',\n");
        sb.append("});");
        return sb.toString();
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
