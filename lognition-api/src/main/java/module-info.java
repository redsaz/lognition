/*
 * Copyright 2022 Redsaz <redsaz@gmail.com>.
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

module LognitionApi {
  exports com.redsaz.lognition.api;
  exports com.redsaz.lognition.api.exceptions;
  exports com.redsaz.lognition.api.labelselector;
  exports com.redsaz.lognition.api.model;
  exports com.redsaz.lognition.api.util;

  requires com.fasterxml.jackson.annotation;
  requires java.desktop;
  requires univocity.parsers;
}
