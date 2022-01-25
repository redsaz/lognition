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
import java.beans.ConstructorProperties;
import java.util.Objects;

/**
 * A review is a collection of logs and charts for the purposes of studying and comparing the logs.
 * For example, if one or more logs are the baseline (a.k.a. control or "before") runs, and another
 * set of logs are experimental runs, then a review can reference these logs in order to research
 * whatever is needed.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class Review {

  private final long id;
  private final String uriName;
  private final String name;
  private final String description;
  private final long createdMillis;
  private final Long lastUpdatedMillis;
  private final String body;

  @JsonCreator
  @ConstructorProperties({
    "id",
    "uriName",
    "name",
    "description",
    "createdMillis",
    "lastUpdatedMillis",
    "body"
  })
  public Review(
      @JsonProperty("id") long inId,
      @JsonProperty("uriName") String inUriName,
      @JsonProperty("name") String inName,
      @JsonProperty("description") String inDescription,
      @JsonProperty("createdMillis") long inCreatedMillis,
      @JsonProperty("lastUpdatedMillis") Long inLastUpdatedMillis,
      @JsonProperty("body") String inBody) {
    id = inId;
    uriName = inUriName;
    name = inName;
    description = inDescription;
    createdMillis = inCreatedMillis;
    lastUpdatedMillis = inLastUpdatedMillis;
    body = inBody;
  }

  public long getId() {
    return id;
  }

  public String getUriName() {
    return uriName;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public long getCreatedMillis() {
    return createdMillis;
  }

  public Long getLastUpdatedMillis() {
    return lastUpdatedMillis;
  }

  public String getBody() {
    return body;
  }

  @Override
  public String toString() {
    return "review_id="
        + id
        + " uriName="
        + uriName
        + " name="
        + name
        + " description="
        + description
        + " createdMillis="
        + createdMillis
        + " lastUpdatedMillis="
        + lastUpdatedMillis
        + " body="
        + body;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(id)
        ^ Objects.hashCode(uriName)
        ^ Objects.hashCode(name)
        ^ Objects.hashCode(description)
        ^ Long.hashCode(createdMillis)
        ^ Objects.hashCode(lastUpdatedMillis)
        ^ Objects.hashCode(body);
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
    final Review other = (Review) obj;
    if (this.id != other.id) {
      return false;
    } else if (!Objects.equals(this.uriName, other.uriName)) {
      return false;
    } else if (!Objects.equals(this.name, other.name)) {
      return false;
    } else if (!Objects.equals(this.description, other.description)) {
      return false;
    } else if (!Objects.equals(this.body, other.body)) {
      return false;
    }
    return true;
  }
}
