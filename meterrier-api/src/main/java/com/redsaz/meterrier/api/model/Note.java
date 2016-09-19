/**
 * Copyright 2016 Redsaz <redsaz@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.redsaz.meterrier.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains the title and content of a note.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class Note {

    private final long id;
    private final String uriName;
    private final String title;
    private final String body;

    @JsonCreator
    public Note(
            @JsonProperty("id") long inId,
            @JsonProperty("uriName") String inUriName,
            @JsonProperty("title") String inTitle,
            @JsonProperty("body") String inBody) {
        id = inId;
        uriName = inUriName;
        title = inTitle;
        body = inBody;
    }

    public long getId() {
        return id;
    }

    public String getUriName() {
        return uriName;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

}
