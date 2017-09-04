/**
 * Copyright 2016 Redsaz <redsaz@gmail.com>.
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
    private final Status status;
    private final String uriName;
    private final String title;
    private final String dataFile;
    private final String notes;

    @JsonCreator
    @ConstructorProperties({"id", "uriName", "title", "notes"})
    public Log(
            @JsonProperty("id") long inId,
            @JsonProperty("status") Status inStatus,
            @JsonProperty("uriName") String inUriName,
            @JsonProperty("title") String inTitle,
            @JsonProperty("dataFile") String inDataFile,
            @JsonProperty("notes") String inNotes) {
        id = inId;
        status = inStatus;
        uriName = inUriName;
        title = inTitle;
        dataFile = inDataFile;
        notes = inNotes;
    }

    public static Log emptyLog() {
        return new Log(0, Status.UNSPECIFIED, null, null, null, null);
    }

    public long getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public String getUriName() {
        return uriName;
    }

    public String getTitle() {
        return title;
    }

    public String getDataFile() {
        return dataFile;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public String toString() {
        return "log_id=" + id
                + " status=" + status
                + " uriName=" + uriName
                + " title=" + title
                + " dataFile=" + dataFile
                + " notes=" + notes;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id)
                ^ Objects.hashCode(status)
                ^ Objects.hashCode(uriName)
                ^ Objects.hashCode(title)
                ^ Objects.hashCode(dataFile)
                ^ Objects.hashCode(notes);
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
        } else if (!Objects.equals(this.status, other.status)) {
            return false;
        } else if (!Objects.equals(this.uriName, other.uriName)) {
            return false;
        } else if (!Objects.equals(this.title, other.title)) {
            return false;
        } else if (!Objects.equals(this.dataFile, other.dataFile)) {
            return false;
        } else if (!Objects.equals(this.notes, other.notes)) {
            return false;
        }
        return true;
    }

    public static enum Status {
        UNSPECIFIED,
        AWAITING_UPLOAD,
        IMPORTING,
        FINISHED
    }
}
