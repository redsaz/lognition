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

module LognitionApp {
  requires LognitionCore;
  requires LognitionApi;
  requires LognitionStoreJooq;
  requires LognitionConvert;
  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;
  requires commons.math3;
  requires freemarker;
  requires io.vertx.core;
  requires java.activation;
  requires java.desktop;
  requires java.ws.rs;
  requires jakarta.enterprise.cdi.api;
  requires jakarta.inject.api;
  requires logback.classic;
  requires org.apache.commons.lang3;
  requires org.commonmark;
  requires org.jooq;
  requires quarkus.jackson;
  requires resteasy.multipart.provider;
  requires slugify;
}
