-- Copyright 2023 Redsaz <redsaz@gmail.com>.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.


CREATE TABLE IF NOT EXISTS log (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1) NOT NULL,
	status INT NOT NULL,
	uri_name VARCHAR(1024) NOT NULL,
	name VARCHAR(1024) NOT NULL,
	data_file VARCHAR(256),
	notes CLOB,
	CONSTRAINT pk_log PRIMARY KEY (id));

CREATE TABLE IF NOT EXISTS import_info (
	id BIGINT NOT NULL,
	imported_filename VARCHAR(1024) NOT NULL,
	uploaded_utc_millis BIGINT NOT NULL,
	CONSTRAINT pk_import_info PRIMARY KEY (id),
	CONSTRAINT fk_import_info_log FOREIGN KEY (id) REFERENCES log(id) ON DELETE CASCADE);

CREATE TABLE IF NOT EXISTS sample_label (
	log_id BIGINT NOT NULL,
	label_id BIGINT NOT NULL,
	label VARCHAR(255) NOT NULL,
	CONSTRAINT pk_sample_label PRIMARY KEY (log_id, label_id),
	CONSTRAINT fk_sample_label_log FOREIGN KEY (log_id) REFERENCES log(id) ON DELETE CASCADE);

CREATE TABLE IF NOT EXISTS aggregate (
	log_id BIGINT NOT NULL,
	label_id BIGINT NOT NULL,
	"MIN" BIGINT,
	p25 BIGINT,
	p50 BIGINT,
	p75 BIGINT,
	p90 BIGINT,
	p95 BIGINT,
	p99 BIGINT,
	"MAX" BIGINT,
	"AVG" BIGINT,
	num_samples BIGINT NOT NULL,
	total_response_bytes BIGINT NOT NULL,
	num_errors BIGINT NOT NULL,
	CONSTRAINT pk_aggregate PRIMARY KEY (log_id, label_id),
	CONSTRAINT fk_aggregate_sample_label FOREIGN KEY (log_id, label_id) REFERENCES sample_label (log_id, label_id) ON DELETE CASCADE);

CREATE TABLE IF NOT EXISTS timeseries (
	log_id BIGINT NOT NULL,
	label_id BIGINT NOT NULL,
	span_millis BIGINT NOT NULL,
	series_data BLOB,
	CONSTRAINT pk_timeseries PRIMARY KEY (log_id, label_id, span_millis),
	CONSTRAINT fk_timeseries_sample_label FOREIGN KEY (log_id, label_id) REFERENCES sample_label (log_id, label_id) ON DELETE CASCADE);

CREATE TABLE IF NOT EXISTS histogram (
	log_id BIGINT NOT NULL,
	label_id BIGINT NOT NULL,
	series_data BLOB,
	CONSTRAINT pk_histogram PRIMARY KEY (log_id, label_id),
	CONSTRAINT fk_histogram_sample_label FOREIGN KEY (log_id, label_id) REFERENCES sample_label (log_id, label_id) ON DELETE CASCADE);

CREATE TABLE IF NOT EXISTS percentile (
	log_id BIGINT NOT NULL,
	label_id BIGINT NOT NULL,
	series_data BLOB,
	CONSTRAINT pk_percentile PRIMARY KEY (log_id, label_id),
	CONSTRAINT fk_percentile_sample_label FOREIGN KEY (log_id, label_id) REFERENCES sample_label (log_id, label_id) ON DELETE CASCADE);

CREATE TABLE IF NOT EXISTS label (
	log_id BIGINT NOT NULL,
	key VARCHAR(63) NOT NULL, "VALUE" VARCHAR(63) NOT NULL,
	CONSTRAINT fk_label_log FOREIGN KEY (log_id) REFERENCES log(id) ON DELETE CASCADE,
	CONSTRAINT pk_label_log_key PRIMARY KEY (log_id, key));
CREATE INDEX IF NOT EXISTS idx_label_log ON label(log_id);
CREATE INDEX IF NOT EXISTS idx_label_key ON label(key);

CREATE TABLE IF NOT EXISTS review (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1) NOT NULL,
	uri_name VARCHAR(1024) NOT NULL,
	name VARCHAR(1024) NOT NULL,
	description VARCHAR(1024) NOT NULL,
	created_millis BIGINT NOT NULL,
	last_updated_millis BIGINT,
	body CLOB,
	CONSTRAINT PK_REVIEW PRIMARY KEY (id));

CREATE TABLE IF NOT EXISTS review_log (
	review_id BIGINT NOT NULL,
	log_id BIGINT NOT NULL,
	CONSTRAINT pk_review_log_key PRIMARY KEY (review_id, log_id),
	CONSTRAINT fk_review_log_log FOREIGN KEY (log_id) REFERENCES log(id) ON DELETE CASCADE,
	CONSTRAINT fk_review_log_review FOREIGN KEY (review_id) REFERENCES review(id) ON DELETE CASCADE);
CREATE INDEX IF NOT EXISTS idx_review_log_review ON review_log(review_id);

CREATE TABLE IF NOT EXISTS code_count (
	log_id BIGINT NOT NULL,
	label_id BIGINT NOT NULL,
	span_millis BIGINT NOT NULL,
	count_data BLOB,
	CONSTRAINT pk_code_count PRIMARY KEY (log_id, label_id, span_millis),
	CONSTRAINT fk_code_count_sample_label FOREIGN KEY (log_id, label_id) REFERENCES sample_label (log_id, label_id) ON DELETE CASCADE);

CREATE TABLE IF NOT EXISTS attachment (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1) NOT NULL,
	owner VARCHAR(1024) NOT NULL,
	path VARCHAR(1024) NOT NULL,
	name VARCHAR(1024) NOT NULL,
	description VARCHAR(1024) NOT NULL,
	mime_type VARCHAR(1024) NOT NULL,
	uploaded_utc_millis BIGINT NOT NULL,
	CONSTRAINT PK_ATTACHMENT PRIMARY KEY (id));
CREATE UNIQUE INDEX IF NOT EXISTS idx_attachment_owner_path ON attachment(owner, path);