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
package com.redsaz.lognition.api.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper methods for creating immutable objects.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class Immutables {

  private Immutables() {}

  /**
   * Creates a new unmodifiable list from the original list with the same contents (not a deep
   * copy), or null if the original list is null.
   *
   * @param <T>
   * @param original Original list
   * @return null if original list is null, or a new list if not.
   */
  public static <T> List<T> listOrNull(List<T> original) {
    if (original == null) {
      return original;
    }
    return Collections.unmodifiableList(new ArrayList<>(original));
  }

  /**
   * Creates a new unmodifiable list from the original list with the same contents (not a deep
   * copy), or an empty list if the original was null.
   *
   * @param <T>
   * @param original Original list
   * @return empty list if original list is null, or a new list if not.
   */
  public static <T> List<T> listOrEmpty(List<T> original) {
    if (original == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(new ArrayList<>(original));
  }
}
