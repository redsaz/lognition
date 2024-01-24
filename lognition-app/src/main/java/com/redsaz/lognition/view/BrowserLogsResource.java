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
import com.redsaz.lognition.api.ReviewsService;
import com.redsaz.lognition.api.StatsService;
import com.redsaz.lognition.api.exceptions.AppClientException;
import com.redsaz.lognition.api.labelselector.LabelSelectorExpression;
import com.redsaz.lognition.api.labelselector.LabelSelectorSyntaxException;
import com.redsaz.lognition.api.model.CodeCounts;
import com.redsaz.lognition.api.model.Histogram;
import com.redsaz.lognition.api.model.ImportInfo;
import com.redsaz.lognition.api.model.Label;
import com.redsaz.lognition.api.model.Log;
import com.redsaz.lognition.api.model.LogBrief;
import com.redsaz.lognition.api.model.Percentiles;
import com.redsaz.lognition.api.model.Review;
import com.redsaz.lognition.api.model.Stats;
import com.redsaz.lognition.api.model.Timeseries;
import com.redsaz.lognition.services.LabelSelectorParser;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
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

  private ReviewsService reviewsSrv;
  private LogsService logsSrv;
  private ImportService importSrv;
  private StatsService statsSrv;
  private Templater cfg;

  private static final Parser CM_PARSER = Parser.builder().build();
  private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().escapeHtml(true).build();
  private static final ExecutorService REVIEWS_CALC_EXEC = Executors.newSingleThreadExecutor();

  public BrowserLogsResource() {}

  @Inject
  public BrowserLogsResource(
      @Sanitizer ReviewsService reviewsService,
      @Sanitizer LogsService logsService,
      @Processor ImportService importService,
      StatsService statsService,
      Templater config) {
    reviewsSrv = reviewsService;
    logsSrv = logsService;
    importSrv = importService;
    statsSrv = statsService;
    cfg = config;
  }

  /**
   * Presents a web page of {@link LogBrief}s.
   *
   * @return Web page of LogBriefs.
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  public Response listLogBriefs() {
    String dist = "/dist";
    List<Log> logs = logsSrv.list();

    Map<String, Object> root = new HashMap<>();
    root.put("briefs", logs);
    root.put("base", "");
    root.put("dist", dist);
    root.put("title", "Logs");
    root.put("content", "log-list.ftl");
    return Response.ok(cfg.buildFromTemplate(root, "base.ftl")).build();
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
  public Response getLogBriefById(@Context HttpServerRequest req, @PathParam("id") long logId) {
    return getLogBrief(logId, null);
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
  public Response getLogBriefByIdWithName(
      @Context HttpServerRequest req,
      @PathParam("id") long logId,
      @PathParam("urlName") String urlName) {
    return getLogBrief(logId, urlName);
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
      List<Label> labels = Collections.emptyList();
      Log sourceLog = null;
      Log resultLog = null;
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

              try {
                sourceLog = new Log(0L, Log.Status.AWAITING_UPLOAD, null, name, null, notes);
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
          case "labels":
            try {
              labels = toLabelsList(part.getBodyAsString());
            } catch (IOException ex) {
              LOGGER.error("Error getting labels.", ex);
            }
            break;
          default:
            {
              // Skip it, we don't use it.
              LOGGER.info("Skipped part={}", subParts.getName());
            }
            break;
        }
      }

      if (labels != null) {
        logsSrv.setLabels(resultLog.getId(), labels);
      }

      // If the name or notes came in AFTER the content was uploaded and the log created, then
      // update the log's name and notes.
      if (!Objects.equals(name, sourceLog.getName())
          || !Objects.equals(notes, sourceLog.getNotes())) {
        Log updatedLog =
            new Log(resultLog.getId(), null, name, name, resultLog.getDataFile(), notes);
        resultLog = logsSrv.update(updatedLog);
      }

      REVIEWS_CALC_EXEC.execute(
          () -> {
            calculateAllReviewLogs();
          });

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
   * @return log upload page.
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("import")
  public Response importLog() {
    String dist = "/dist";
    Map<String, Object> root = new HashMap<>();
    root.put("base", "");
    root.put("dist", dist);
    root.put("title", "Import Log");
    root.put("content", "log-import.ftl");
    return Response.ok(cfg.buildFromTemplate(root, "base.ftl")).build();
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
  public Response editLog(@Context HttpServerRequest httpRequest, @PathParam("id") long logId) {
    String dist = "/dist";

    Map<String, Object> root = new HashMap<>();
    Log log = logsSrv.get(logId);
    if (log == null) {
      throw new NotFoundException("Could not find logId=" + logId);
    }
    List<Label> labels = logsSrv.getLabels(logId);
    StringBuilder labelsText = new StringBuilder();
    labels.stream().forEach(l -> labelsText.append(l.toString()).append(' '));
    // If there's at least one label, trim off the last space.
    if (labelsText.length() > 0) {
      labelsText.setLength(labelsText.length() - 1);
    }

    LOGGER.info("Editing log={}", log);

    root.put("brief", log);
    root.put("labelsText", labelsText);
    root.put("base", "");
    root.put("dist", dist);
    root.put("title", "Edit Log");
    root.put("content", "log-edit.ftl");
    return Response.ok(cfg.buildFromTemplate(root, "base.ftl")).build();
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
      List<Label> labels = Collections.emptyList();
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
          case "labels":
            try {
              labels = toLabelsList(part.getBodyAsString());
            } catch (IOException ex) {
              LOGGER.error("Error getting labels.", ex);
            }
            break;
          default:
            {
              // Skip it, we don't use it.
              LOGGER.info("Skipped part={}", subParts.getName());
            }
            break;
        }
      }
      Log sourceLog = new Log(logId, null, null, name, null, notes);
      logsSrv.update(sourceLog);

      if (labels != null) {
        logsSrv.setLabels(logId, labels);
      }

      REVIEWS_CALC_EXEC.execute(
          () -> {
            calculateAllReviewLogs();
          });

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
   * @param logId The id of the brief.
   * @param uriName The optional log URI name. May be null.
   * @return Brief view page if the urlName matches the reall urlName for the log with the id.
   */
  private Response getLogBrief(long logId, String uriName) {
    String dist = "/dist";
    Log log = logsSrv.get(logId);
    if (log == null) {
      throw new NotFoundException("Could not find logId=" + logId);
    } else if (!Objects.equals(uriName, log.getUriName())
        && log.getUriName() != null
        && !log.getUriName().isEmpty()) {
      LOGGER.debug(
          "logId={} provided name was \"{}\" but expected \"{}\".",
          logId,
          uriName,
          log.getUriName());
      return Response.seeOther(URI.create("logs/" + logId + "/" + log.getUriName()))
          .status(Response.Status.MOVED_PERMANENTLY)
          .build();
    }

    List<String> sampleLabels = statsSrv.getSampleLabels(logId);
    List<String> graphs = new ArrayList<>(sampleLabels.size());
    List<Stats> aggregates = new ArrayList<>(sampleLabels.size());
    List<String> histogramGraphs = new ArrayList<>(sampleLabels.size());
    List<String> percentileGraphs = new ArrayList<>(sampleLabels.size());
    List<String> timeseriesCodeCountGraphs = new ArrayList<>(sampleLabels.size());
    List<CodeCounts> aggregateCodeCounts =
        normalizeCodeCounts(statsSrv.getCodeCountsForLog(logId, 0L));
    Map<Long, CodeCounts> timeseriesCodeCounts = statsSrv.getCodeCountsForLog(logId, 60_000L);

    List<String> errorTimeseriesGraphs = new ArrayList<>(sampleLabels.size());
    List<String> errorPercentTimeseriesGraphs = new ArrayList<>(sampleLabels.size());
    for (int i = 0; i < sampleLabels.size(); ++i) {
      String label = sampleLabels.get(i);

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

      CodeCounts timeseriesCodeCountsForLabel = timeseriesCodeCounts.get((long) i);
      if (timeseriesCodeCountsForLabel != null) {
        String codeCountsGraph =
            createTimeseriesCodeCountsGraph(timeseriesCodeCountsForLabel, label, i);
        timeseriesCodeCountGraphs.add(codeCountsGraph);
      }

      String errorGraph = createTimeseriesErrorGraph(timeseries, label, i);
      errorTimeseriesGraphs.add(errorGraph);

      String errorPercentGraph = createTimeseriesErrorPercentGraph(timeseries, label, i);
      errorPercentTimeseriesGraphs.add(errorPercentGraph);
    }
    List<Label> labels = logsSrv.getLabels(logId);

    Map<String, Object> root = new HashMap<>();
    root.put("brief", log);
    root.put("labels", labels);
    root.put("notesHtml", commonMarkToHtml(log.getNotes()));
    root.put("sampleLabels", sampleLabels);
    root.put("graphs", graphs);
    root.put("aggregates", aggregates);
    root.put("histogramGraphs", histogramGraphs);
    root.put("percentileGraphs", percentileGraphs);
    root.put("errorTimeseriesGraphs", errorTimeseriesGraphs);
    root.put("errorPercentTimeseriesGraphs", errorPercentTimeseriesGraphs);
    if (aggregateCodeCounts.size() > 0) {
      root.put("aggregateCodes", aggregateCodeCounts.get(0).getCodes());
      root.put("aggregateCodeCounts", aggregateCodeCounts);
    }
    root.put("timeseriesCodeCountsGraphs", timeseriesCodeCountGraphs);
    root.put("stylesheets", List.of(dist + "/css/dygraph.css"));
    root.put("base", "");
    root.put("dist", dist);
    root.put("title", log.getName());
    root.put("content", "log-view.ftl");
    return Response.ok(cfg.buildFromTemplate(root, "base.ftl")).build();
  }

  private void calculateAllReviewLogs() {
    for (Review review : reviewsSrv.list()) {
      try {
        String body = review.getBody();
        LabelSelectorExpression labelSelector = LabelSelectorParser.parse(body);
        List<Long> logIds = logsSrv.listIdsBySelector(labelSelector);

        reviewsSrv.setReviewLogs(review.getId(), logIds);
      } catch (LabelSelectorSyntaxException ex) {
        LOGGER.error(
            "Review_id={} has a syntax error with it's label selector. No more logs will be added to the review until fixed.",
            review.getId());
      } catch (RuntimeException ex) {
        LOGGER.error("Exception when using label selector from review_id={}.", ex);
      }
    }
  }

  // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
  private static void parseContentDispositionHeader(
      String headerValue, NameValueListener listener) {
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
    String[] nameValPair = new String[] {null, null};
    for (int length = headerValue.length(); cursor < length && cursor >= 0; ) {
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
    sb.append("new Dygraph(document.getElementById(\"histogramgraphdiv")
        .append(index)
        .append("\"),\n");
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
    sb.append("new Dygraph(document.getElementById(\"percentilegraphdiv")
        .append(index)
        .append("\"),\n");
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

  /**
   * Normalizes each CodeCounts so they have the same number of codes in each.
   *
   * <p>For example, the map.get(0) CodeCounts (which is the "overall" CodeCounts, the sum of all
   * counts) may have codes=>counts of "200"=>10, "404"=>4, "500"=>1, and "503"=>1. The map.get(1)
   * CodeCounts (which is just the CdeCounts for a single label) may have "200"=>4, "404"=>3, and
   * "500"=>1. The map.get(2) CodeCounts may have "200"=>6, "404"=>1, and "503"=>1.
   *
   * <p>This method will return a list where map.get(0) remains unchanged, but map.get(1) will have
   * "200"=>4, "404"=>3, "500"=>1, and "503"=>0, and map.get(2) will have "200"=>6, "404"=>1,
   * "500"=>0, and "503"=>1. This way all of the CodeCounts can be represented by a table, or
   * perhaps by a stacked bar chart. The list is in the order of the keys, so 0 is first, 1 is next,
   * etc.
   *
   * @param originals The codeCounts to normalize
   * @return A list with new CodeCounts whose counts have been normalized, in the natural order of
   *     the keys.
   */
  private static List<CodeCounts> normalizeCodeCounts(Map<Long, CodeCounts> originals) {
    if (originals == null || originals.isEmpty()) {
      return Collections.emptyList();
    }
    // Since overall will by definition have ALL of the code counts, it already has the complete
    // list of codes. Normalize all the CodeCounts to this.
    CodeCounts overall = originals.get(0L);
    List<String> overallCodes = overall.getCodes();

    List<CodeCounts> results = new ArrayList<>(originals.size());
    // The keys in the map are 0 (inclusive) through map.size (exclusive), no gaps.
    results.add(overall);
    for (int i = 1; i < originals.size(); ++i) {
      CodeCounts normalized = originals.get(Long.valueOf(i)).normalizeUsing(overallCodes);
      results.add(normalized);
    }

    return results;
  }

  private static String createTimeseriesCodeCountsGraph(
      CodeCounts codeCounts, String label, int index) {
    if (codeCounts == null) {
      LOGGER.debug("CodeCounts timeseries is empty.");
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("new Dygraph(document.getElementById(\"codecountsgraphdiv")
        .append(index)
        .append("\"),\n");
    String csvRowTail = " +\n";
    sb.append("\"offsetMillis,")
        .append(String.join(",", codeCounts.getCodes()))
        .append("\\n\"")
        .append(csvRowTail);
    int i = 0;
    for (List<Integer> counts : codeCounts.getCounts()) {
      List<String> countStrings =
          counts.stream().map(count -> Integer.toString(count)).collect(Collectors.toList());
      sb.append("\"")
          .append(codeCounts.getSpanMillis() * i)
          .append(',')
          .append(String.join(",", countStrings))
          .append("\\n\"")
          .append(csvRowTail);
      i++;
    }
    if (!codeCounts.getCounts().isEmpty()) {
      sb.setLength(sb.length() - csvRowTail.length());
    }
    sb.append(", {\n");
    sb.append("legend: 'always',\n");
    sb.append("title: '").append(label).append("',\n");
    sb.append("xlabel: 'Time',\n");
    sb.append("ylabel: 'Count',\n");
    sb.append("});");
    return sb.toString();
  }

  private static String createTimeseriesErrorGraph(Timeseries timeseries, String label, int index) {
    if (timeseries == null) {
      LOGGER.debug("Timeseries is empty.");
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("new Dygraph(document.getElementById(\"errorTimeseriesdiv")
        .append(index)
        .append("\"),\n");
    String csvRowTail = " +\n";
    sb.append("\"offsetMillis,numErrors\\n\"").append(csvRowTail);
    List<Stats> statsList = timeseries.getStatsList();
    for (Stats stats : statsList) {
      sb.append("\"")
          .append(stats.getOffsetMillis())
          .append(",")
          .append(stats.getNumErrors())
          .append("\\n\"")
          .append(csvRowTail);
    }
    if (!statsList.isEmpty()) {
      sb.setLength(sb.length() - csvRowTail.length());
    }
    sb.append(", {\n");
    sb.append("legend: 'always',\n");
    sb.append("title: '").append(label).append("',\n");
    sb.append("xlabel: 'Time',\n");
    sb.append("ylabel: 'Error count',\n");
    sb.append("});");
    return sb.toString();
  }

  private static String createTimeseriesErrorPercentGraph(
      Timeseries timeseries, String label, int index) {
    if (timeseries == null) {
      LOGGER.debug("Timeseries is empty.");
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("new Dygraph(document.getElementById(\"errorPercentTimeseriesdiv")
        .append(index)
        .append("\"),\n");
    String csvRowTail = " +\n";
    sb.append("\"offsetMillis,errorPercent\\n\"").append(csvRowTail);
    List<Stats> statsList = timeseries.getStatsList();
    for (Stats stats : statsList) {
      double total = stats.getNumSamples();
      double errors = stats.getNumErrors();
      double perc;
      if (total > 0d) {
        perc = errors * 100d / total;
      } else {
        perc = 0d;
      }
      sb.append("\"")
          .append(stats.getOffsetMillis())
          .append(",")
          .append(perc)
          .append("\\n\"")
          .append(csvRowTail);
    }
    if (!statsList.isEmpty()) {
      sb.setLength(sb.length() - csvRowTail.length());
    }
    sb.append(", {\n");
    sb.append("valueRange: [0, 100],\n");
    sb.append("legend: 'always',\n");
    sb.append("title: '").append(label).append("',\n");
    sb.append("xlabel: 'Time',\n");
    sb.append("ylabel: 'Error %',\n");
    sb.append("});");
    return sb.toString();
  }

  private static List<Label> toLabelsList(String labelsText) {
    LOGGER.info("Labelizing labels=\"{}\"", labelsText);
    if (labelsText == null || labelsText.isEmpty()) {
      return Collections.emptyList();
    }

    String[] pairs = labelsText.split("(?:,|\\s)+");
    List<Label> labels = new ArrayList<>(pairs.length);
    for (String pair : pairs) {
      Label label = toLabel(pair);
      labels.add(label);
    }

    LOGGER.info("Labelized labels: {}", labels);
    return labels;
  }

  private static Label toLabel(String labelText) {
    String[] keyval = labelText.split("=", 2);
    if (keyval.length != 2) {
      return new Label(keyval[0], "");
    }
    return new Label(keyval[0], keyval[1]);
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
