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
import java.util.Objects;

/**
 * The log data.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class Log {

    private final long id;
    private final String uriName;
    private final String title;
    private final long uploadedUtcMillis;
    private final String notes;

    @JsonCreator
    @ConstructorProperties({"storedFilename"})
    public Log(
            @JsonProperty("id") long inId,
            @JsonProperty("uriName") String inUriName,
            @JsonProperty("title") String inTitle,
            @JsonProperty("uploadedUtcMillis") long inUploadedUtcMillis,
            @JsonProperty("notes") String inNotes) {
        id = inId;
        uriName = inUriName;
        title = inTitle;
        uploadedUtcMillis = inUploadedUtcMillis;
        notes = inNotes;
    }

    public static Log emptyLog() {
        return new Log(0, null, null, System.currentTimeMillis(), null);
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

    public long getUploadedUtcMillis() {
        return uploadedUtcMillis;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public String toString() {
        return "log_id=" + id
                + " uriName=" + uriName
                + " title=" + title
                + " uploadedUtcMillis=" + uploadedUtcMillis
                + " notes=" + notes;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id)
                ^ Objects.hashCode(uriName.hashCode())
                ^ Objects.hashCode(title.hashCode())
                ^ Long.hashCode(uploadedUtcMillis)
                ^ Objects.hashCode(notes.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }
        final Log other = (Log) obj;
        if (this.id != other.id) {
            return false;
        } else if (this.uploadedUtcMillis != other.uploadedUtcMillis) {
            return false;
        } else if (!Objects.equals(this.uriName, other.uriName)) {
            return false;
        } else if (!Objects.equals(this.title, other.title)) {
            return false;
        } else if (!Objects.equals(this.notes, other.notes)) {
            return false;
        }
        return true;
    }
}
