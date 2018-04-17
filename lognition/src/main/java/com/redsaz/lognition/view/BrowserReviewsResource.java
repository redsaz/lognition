/*
 * Copyright 2018 Redsaz <redsaz@gmail.com>.
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

import com.redsaz.lognition.api.ReviewsService;
import com.redsaz.lognition.api.model.Review;
import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
 * An endpoint for accessing reviews. The REST endpoints and browser endpoints are identical; look
 * at docs/endpoints.md for why.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
@Path("/reviews")
public class BrowserReviewsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserReviewsResource.class);

    private ReviewsService reviewsSrv;
    private Templater cfg;

    private static final Parser CM_PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().escapeHtml(true).build();

    public BrowserReviewsResource() {
    }

    @Inject
    public BrowserReviewsResource(@Sanitizer ReviewsService reviewsService, Templater config) {
        reviewsSrv = reviewsService;
        cfg = config;
    }

    /**
     * Presents a web page of reviews.
     *
     * @param httpRequest The request for the page.
     * @return Web page of reviews.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response listReviews(@Context HttpServletRequest httpRequest) {
        String base = httpRequest.getContextPath();
        String dist = base + "/dist";
        List<Review> reviews = reviewsSrv.list();
        LOGGER.info("Reviews: {}", reviews);

        Map<String, Object> root = new HashMap<>();
        root.put("reviews", reviews);
        root.put("base", base);
        root.put("dist", dist);
        root.put("title", "Reviews");
        root.put("content", "review-list.ftl");
        return Response.ok(cfg.buildFromTemplate(root, "page.ftl")).build();
    }

    /**
     * Presents a web page for viewing a specific review.
     *
     * @param req The request for the page.
     * @param reviewId The id of the review.
     * @return Review view page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("{id}")
    public Response getReviewById(@Context HttpServletRequest req, @PathParam("id") long reviewId) {
        return getReview(req, reviewId, null);
    }

    /**
     * Presents a web page for viewing a specific review.
     *
     * @param req The request for the review.
     * @param reviewId The id of the review.
     * @param urlName The urlName of the review.
     * @return Review view page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("{id}/{urlName}")
    public Response getReviewByIdWithName(@Context HttpServletRequest req,
            @PathParam("id") long reviewId, @PathParam("urlName") String urlName) {
        return getReview(req, reviewId, urlName);
    }

    @POST
    @Consumes("multipart/form-data")
    @Produces({MediaType.TEXT_HTML})
    public Response finishCreateReview(MultipartInput input) {
        LOGGER.info("Creating review...");
        try {
            String name = null;
            String description = null;
            String body = null;
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
                    case "description":
                        try {
                            description = part.getBodyAsString();
                        } catch (IOException ex) {
                            LOGGER.error("Error getting description.", ex);
                        }
                        break;
                    case "body":
                        try {
                            body = part.getBodyAsString();
                        } catch (IOException ex) {
                            LOGGER.error("Error getting body.", ex);
                        }
                        break;
                    default: {
                        // Skip it, we don't use it.
                        LOGGER.info("Skipped part={}", subParts.getName());
                    }
                    break;
                }
            }
            Review review = new Review(0, null, name, description,
                    ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond(), null, body);
            Review result = reviewsSrv.create(review);
            Response resp = Response.seeOther(URI.create("reviews")).build();
            LOGGER.info("Finished creating review {}", result);
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
     * Presents a web page for creating a review.
     *
     * @param httpRequest The request for the page.
     * @return review creation page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("create")
    public Response createReview(@Context HttpServletRequest httpRequest) {
        String base = httpRequest.getContextPath();
        String dist = base + "/dist";
        Map<String, Object> root = new HashMap<>();
        root.put("base", base);
        root.put("dist", dist);
        root.put("title", "Create Review");
        root.put("content", "review-create.ftl");
        return Response.ok(cfg.buildFromTemplate(root, "page.ftl")).build();
    }

    /**
     * Presents a web page for editing a review.
     *
     * @param httpRequest The request for the page.
     * @param reviewId the review to edit.
     * @return review edit page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("{id}/edit")
    public Response editReview(@Context HttpServletRequest httpRequest, @PathParam("id") long reviewId) {
        String base = httpRequest.getContextPath();
        String dist = base + "/dist";

        Map<String, Object> root = new HashMap<>();
        Review review = reviewsSrv.get(reviewId);
        if (review == null) {
            throw new NotFoundException("Could not find reviewId=" + reviewId);
        }

        LOGGER.info("Editing review={}", review);

        root.put("review", review);
        root.put("base", base);
        root.put("dist", dist);
        root.put("title", "Edit Review");
        root.put("content", "review-edit.ftl");
        return Response.ok(cfg.buildFromTemplate(root, "page.ftl")).build();
    }

    @POST
    @Consumes("multipart/form-data")
    @Produces(MediaType.TEXT_HTML)
    @Path("{id}")
    public Response finishEditReview(MultipartInput input, @PathParam("id") long reviewId) {
        LOGGER.info("Submitting review {} edits...", reviewId);
        try {
            String name = null;
            String description = null;
            String body = null;
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
                    case "description":
                        try {
                            description = part.getBodyAsString();
                        } catch (IOException ex) {
                            LOGGER.error("Error getting description.", ex);
                        }
                        break;
                    case "body":
                        try {
                            body = part.getBodyAsString();
                        } catch (IOException ex) {
                            LOGGER.error("Error getting body.", ex);
                        }
                        break;
                    default: {
                        // Skip it, we don't use it.
                        LOGGER.info("Skipped part={}", subParts.getName());
                    }
                    break;
                }
            }
            Review review = new Review(reviewId, null, name, description,
                    ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond(), null, body);
            reviewsSrv.update(review);
            Response resp = Response.seeOther(URI.create("reviews/" + reviewId)).build();
            LOGGER.info("Finished updating review {}.", reviewId);
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
    public Response deleteReview(@FormParam("id") long id) {
        reviewsSrv.delete(id);
        Response resp = Response.seeOther(URI.create("/reviews")).build();
        return resp;
    }

    /**
     * Presents a web page for viewing a specific review. The actual review displayed only really
     * depends on the review id, the URL name is optional. If the given URL name doesn't match the
     * real URL name of the review with the id, then a redirect will happen.
     *
     * @param req The request for the page.
     * @param reviewId The id of the review.
     * @param uriName The optional review URI name. May be null.
     * @return Review view page if the urlName matches the real urlName for the review with the id.
     */
    private Response getReview(HttpServletRequest req, long reviewId, String uriName) {
        String base = req.getContextPath();
        String dist = base + "/dist";
        Review review = reviewsSrv.get(reviewId);
        if (review == null) {
            throw new NotFoundException("Could not find reviewId=" + reviewId);
        } else if (!Objects.equals(uriName, review.getUriName())
                && review.getUriName() != null
                && !review.getUriName().isEmpty()) {
            LOGGER.debug("reviewId={} provided name was \"{}\" but expected \"{}\".", reviewId, uriName, review.getUriName());
            return Response.seeOther(URI.create("reviews/" + reviewId + "/" + review.getUriName()))
                    .status(Response.Status.MOVED_PERMANENTLY)
                    .build();
        }

        Map<String, Object> root = new HashMap<>();
        root.put("review", review);
        root.put("base", base);
        root.put("dist", dist);
        root.put("title", review.getName());
        root.put("descriptionHtml", commonMarkToHtml(review.getDescription()));
        root.put("content", "review-view.ftl");
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
