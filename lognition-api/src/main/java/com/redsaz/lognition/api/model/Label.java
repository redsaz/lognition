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
package com.redsaz.lognition.api.model;

import java.util.Objects;

/**
 * A label is a key/value pair that can be attached to logs. Labels are how reports can select which
 * logs to report on. The key is required and must be no longer than 63-characters. It may start and
 * end with alphanumeric characters [a-zA-Z0-9] and can have alphanumeric characters, dashes (-),
 * and periods (.) in between. The value is optional, it can be either an empty String or can start
 * and end with alphanumeric characters with alphanumeric characters, dashes, and periods in
 * between.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class Label implements Comparable<Label> {

  private final String key;
  private final String value;

  public Label(String inKey, String inValue) {
    key = inKey;
    value = inValue;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    if (value.isEmpty()) {
      return key;
    }
    return key + "=" + value;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Label)) {
      return false;
    }
    Label right = (Label) obj;
    return Objects.equals(key, right.key) && Objects.equals(value, right.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @Override
  public int compareTo(Label o) {
    if (o == null) {
      return -1;
    }
    return this.getKey().compareTo(o.getKey());
  }
}
