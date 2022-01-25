/**
 * Copyright 2016 Redsaz <redsaz@gmail.com>.
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
import java.util.Objects;

/**
 * Details on an uploaded file that still needs imported.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class ImportInfo {

  private final long id;
  private final String importedFilename;
  private final long uploadedUtcMillis;

  @JsonCreator
  public ImportInfo(
      @JsonProperty("id") long inId,
      @JsonProperty("importedFilename") String inImportedFilename,
      @JsonProperty("uploadedUtcMillis") long inUploadedUtcMillis) {
    id = inId;
    importedFilename = inImportedFilename;
    uploadedUtcMillis = inUploadedUtcMillis;
  }

  public long getId() {
    return id;
  }

  public String getImportedFilename() {
    return importedFilename;
  }

  public long getUploadedUtcMillis() {
    return uploadedUtcMillis;
  }

  @Override
  public String toString() {
    return "imported_id="
        + id
        + " importedFilename="
        + importedFilename
        + " uploadedUtcMillis="
        + uploadedUtcMillis;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(id) ^ Long.hashCode(uploadedUtcMillis) ^ Objects.hash(importedFilename);
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
    final ImportInfo other = (ImportInfo) obj;
    return this.id == other.id
        && this.uploadedUtcMillis == other.uploadedUtcMillis
        && Objects.equals(this.importedFilename, other.importedFilename);
  }
}
