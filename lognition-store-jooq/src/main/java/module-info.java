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

module LognitionStoreJooq {
  exports com.redsaz.lognition.store;
  exports com.redsaz.lognition.model.tables.records;

  opens db.migration;

  requires LognitionApi;
  requires transitive org.hsqldb;
  requires org.slf4j;
  requires org.jooq;
  requires java.naming;
  requires flyway.core;

  uses org.hsqldb.jdbc.JDBCDriver;
  uses org.flywaydb.core.extensibility.Plugin;
}
