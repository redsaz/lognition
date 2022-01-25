/**
 * Copyright 2018 Redsaz <redsaz@gmail.com>.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redsaz.lognition.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.beans.ConstructorProperties;

/**
 * Contains settings and logic for reviewing/comparing logs.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
@JsonPropertyOrder({"type", "title", "notes", "logLabelSelector", "logDisplayOrder", "resolution"})
public class ReviewPanel {

  private final String type;
  private final String title;
  private final String notes;
  private final String logLabelSelector;
  private final String logDisplayOrder;
  private final String resolution;

  @JsonCreator
  @ConstructorProperties({
    "type",
    "title",
    "notes",
    "logLabelSelector",
    "logDisplayOrder",
    "resolution"
  })
  public ReviewPanel(
      @JsonProperty("type") String inType,
      @JsonProperty("title") String inTitle,
      @JsonProperty("notes") String inNotes,
      @JsonProperty("logLabelSelector") String inLogLabelSelector,
      @JsonProperty("logDisplayOrder") String inLogDisplayOrder,
      @JsonProperty("resolution") String inResolution) {
    type = inType;
    title = inTitle;
    notes = inNotes;
    logLabelSelector = inLogLabelSelector;
    logDisplayOrder = inLogDisplayOrder;
    resolution = inResolution;
  }

  public String getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getNotes() {
    return notes;
  }

  public String getLogLabelSelector() {
    return logLabelSelector;
  }

  public String getLogDisplayOrder() {
    return logDisplayOrder;
  }

  public String getResolution() {
    return resolution;
  }
}
