/**
 * Copyright 2018 Redsaz <redsaz@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.redsaz.lognition.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redsaz.lognition.api.util.Immutables;
import java.beans.ConstructorProperties;
import java.util.List;

/**
 * Contains the panels and logic for reviews.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class ReviewBody {

    private final String title;
    private final String notes;
    private final String baseLogLabelSelector;
    private final List<ReviewPanel> panels;

    @JsonCreator
    @ConstructorProperties({"title", "notes", "baseLogLabelSelector", "panels"})
    public ReviewBody(
            @JsonProperty("title") String inTitle,
            @JsonProperty("notes") String inNotes,
            @JsonProperty("baseLogLabelSelector") String inBaseLogLabelSelector,
            @JsonProperty("panels") List<ReviewPanel> inPanels) {
        title = inTitle;
        notes = inNotes;
        baseLogLabelSelector = inBaseLogLabelSelector;
        panels = Immutables.listOrEmpty(inPanels);

    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }

    public String getBaseLogLabelSelector() {
        return baseLogLabelSelector;
    }

    public List<ReviewPanel> getPanels() {
        return panels;
    }

}
