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

import com.redsaz.lognition.api.LognitionMediaType;
import com.redsaz.lognition.api.LogsService;
import com.redsaz.lognition.api.ReviewsService;
import com.redsaz.lognition.api.labelselector.LabelSelectorExpression;
import com.redsaz.lognition.api.labelselector.LabelSelectorExpressionFormatter;
import com.redsaz.lognition.api.labelselector.LabelSelectorSyntaxException;
import com.redsaz.lognition.api.model.Log;
import com.redsaz.lognition.api.model.Review;
import com.redsaz.lognition.services.LabelSelectorParser;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An endpoint for accessing reviews. Many of the REST endpoints and browser endpoints are identical
 * where possible; look at docs/endpoints.md for why.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
@Path("/reviews")
public class ReviewsResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReviewsResource.class);

  private ReviewsService reviewsSrv;
  private LogsService logsSrv;

  public ReviewsResource() {}

  @Inject
  public ReviewsResource(
      @Sanitizer ReviewsService reviewsService, @Sanitizer LogsService logsService) {
    reviewsSrv = reviewsService;
    logsSrv = logsService;
  }

  /**
   * Lists all of the reviews.
   *
   * @return List of reviews.
   */
  @GET
  @Produces({LognitionMediaType.REVIEW_V1_JSON, MediaType.APPLICATION_JSON})
  public Response listReviews() {
    return Response.ok(reviewsSrv.list()).build();
  }

  /**
   * Get a review by id.
   *
   * @param id The id of the review.
   * @return a review.
   */
  @GET
  @Produces({LognitionMediaType.REVIEW_V1_JSON, MediaType.APPLICATION_JSON})
  @Path("{id}")
  public Response getReview(@PathParam("id") long id) {
    Review review = reviewsSrv.get(id);
    if (review == null) {
      throw new NotFoundException("Could not find review id=" + id);
    }
    return Response.ok(review).build();
  }

  @POST
  @Consumes({LognitionMediaType.REVIEW_V1_JSON, MediaType.APPLICATION_JSON})
  @Produces({LognitionMediaType.REVIEW_V1_JSON, MediaType.APPLICATION_JSON})
  public Response createReview(Review received) {
    LOGGER.info("Creating review...");
    // Only the name, uriName, description, and label selector from the received review should
    // be used since the rest are calculated by the app.
    String body =
        LabelSelectorExpressionFormatter.format(LabelSelectorParser.parse(received.getBody()));

    // TODO hey whoops we store millis not seconds, fix it later. For now this matches what
    // has been done in lognition for quite some time now.
    long now = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond();

    Review review =
        new Review(
            0,
            received.getUriName(),
            received.getName(),
            received.getDescription(),
            now,
            now,
            body);
    Review result = reviewsSrv.create(review);

    // TODO calculation of the logs can be done asynchronously.
    calculateReviewLogs(result);
    Response resp =
        Response.created(URI.create("/reviews/" + result.getId())).entity(result).build();
    LOGGER.info("Finished creating review {}", result);
    return resp;
  }

  /**
   * Delete a review.
   *
   * @param id The id of the review.
   * @return No content response.
   */
  @DELETE
  @Path("{id}")
  public Response deleteReview(@PathParam("id") long id) {
    reviewsSrv.delete(id);
    return Response.status(Status.NO_CONTENT).build();
  }

  @GET
  @Path("{id}/logs")
  @Produces({LognitionMediaType.LOGBRIEF_V1_JSON, MediaType.APPLICATION_JSON})
  public Response listLogs(@PathParam("id") long id) {
    List<Log> logs = reviewsSrv.getReviewLogs(id);
    return Response.ok(logs).build();
  }

  private void calculateReviewLogs(Review review) {
    try {
      String body = review.getBody();
      LabelSelectorExpression labelSelector = LabelSelectorParser.parse(body);
      List<Long> logIds = logsSrv.listIdsBySelector(labelSelector);

      reviewsSrv.setReviewLogs(review.getId(), logIds);
    } catch (LabelSelectorSyntaxException ex) {
      LOGGER.error(
          "Could not find logs for review due to Syntax error in label selector for review_id={}",
          review.getId());
    }
  }
}
