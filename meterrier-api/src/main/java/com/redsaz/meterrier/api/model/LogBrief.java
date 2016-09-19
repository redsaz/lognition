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
import java.beans.ConstructorProperties;

/**
 * Metadata about a log. This can include filename, number of samples, notes,
 * start time, end time, etc.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class LogBrief {

    private final long id;
    private final String uriName;
    private final String title;
    private final String notes;
    private final String filename;
    private final long uploadedTimestampMillis;
    private final long contentId;

    @JsonCreator
    @ConstructorProperties({"id", "uriName", "title", "notes", "filename", "uploadedTimestampMillis"})
    public LogBrief(
            @JsonProperty("id") long inId,
            @JsonProperty("uriName") String inUriName,
            @JsonProperty("title") String inTitle,
            @JsonProperty("notes") String inNotes,
            @JsonProperty("filename") String inFilename,
            @JsonProperty("uploadedTimestampMillis") long inUploadedTimestampMillis,
            @JsonProperty("contentId") long inContentId) {
        id = inId;
        uriName = inUriName;
        title = inTitle;
        notes = inNotes;
        filename = inFilename;
        uploadedTimestampMillis = inUploadedTimestampMillis;
        contentId = inContentId;
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

    public String getNotes() {
        return notes;
    }

    public String getFilename() {
        return filename;
    }

    public long getUploadedTimestampMillis() {
        return uploadedTimestampMillis;
    }

    public long getContentId() {
        return contentId;
    }

}
