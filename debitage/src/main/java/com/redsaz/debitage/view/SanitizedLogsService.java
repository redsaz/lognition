/*
 * Copyright 2016 Redsaz <redsaz@gmail.com>.
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
package com.redsaz.debitage.view;

import com.github.slugify.Slugify;
import com.redsaz.debitage.api.exceptions.AppClientException;
import com.redsaz.debitage.api.model.LogBrief;
import com.redsaz.debitage.api.model.Log;
import com.redsaz.debitage.store.HsqlLogsService;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import com.redsaz.debitage.api.LogsService;

/**
 * Does not directly store logs, but is responsible for ensuring that the logs
 * and metadata sent to and retrieved from the store are correctly formatted,
 * sized, and without malicious/errorific content.
 *
 * Default values for jtl files:
 * timestamp,elapsed,label,responseCode,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency
 *
 * @author Redsaz <redsaz@gmail.com>
 */
@Default
@ApplicationScoped
public class SanitizedLogsService implements LogsService {

    private static final Slugify SLG = initSlug();
    private static final int SHORTENED_MAX = 60;
    private static final int SHORTENED_MIN = 12;

    private final LogsService srv;

    public SanitizedLogsService() {
        srv = new HsqlLogsService();
    }

//    @Override
//    public List<LogBrief> getLogBriefs() {
//        return sanitizeAll(srv.getLogBriefs());
//    }
//
//    @Override
//    public LogBrief getLogBrief(long id) {
//        return sanitize(srv.getLogBrief(id));
//    }
    @Override
    public OutputStream getLogContent(long id) {
        return srv.getLogContent(id);
    }

//    @Override
//    public LogBrief createBrief(LogBrief source) {
//        if (source == null) {
//            throw new AppClientException("No log brief provided.");
//        }
//        return srv.createBrief(sanitize(source));
//    }
//
//    @Override
//    public Log createContent(InputStream raw) {
//        if (raw == null) {
//            throw new AppClientException("No log provided.");
//        }
//        return srv.createContent(raw);
//    }
    @Override
    public void deleteLog(long id) {
        srv.deleteLog(id);
    }

    /**
     * Sanitizes a group of notes according to the
     * {@link #sanitize(com.redsaz.debitage.api.model.Note)} method.
     *
     * @param notes The notes to sanitize
     * @return A List of new note instances with sanitized data.
     */
    private static List<LogBrief> sanitizeAll(List<LogBrief> briefs) {
        List<LogBrief> sanitizeds = new ArrayList<>(briefs.size());
        for (LogBrief brief : briefs) {
            sanitizeds.add(sanitize(brief));
        }
        return sanitizeds;
    }

    /**
     * A brief must have at least a uri and a title. If neither are present, nor
     * filename or notes, then it cannot be sanitized. The ID will remain
     * unchanged.
     *
     * @param brief The brief to sanitize
     * @return A new brief instance with sanitized data.
     */
    private static LogBrief sanitize(LogBrief brief) {
        String uriName = brief.getUriName();
        if (uriName == null || uriName.isEmpty()) {
            uriName = brief.getTitle();
            if (uriName == null || uriName.isEmpty()) {
                uriName = shortened(brief.getFilename());
                if (uriName == null || uriName.isEmpty()) {
                    uriName = shortened(brief.getNotes());
                    if (uriName == null || uriName.isEmpty()) {
                        throw new AppClientException("Brief must have at least a uri, title, filename, or notes.");
                    }
                }
            }
        }
        uriName = SLG.slugify(uriName);

        String title = brief.getTitle();
        if (title == null) {
            title = shortened(brief.getNotes());
            if (title == null) {
                title = "";
            }
        }
        String notes = brief.getNotes();
        if (notes == null) {
            notes = "";
        }
        String filename = brief.getFilename();
        if (filename == null) {
            filename = "";
        }

        return new LogBrief(brief.getId(), uriName, title, notes, filename, brief.getUploadedTimestampMillis(), brief.getContentId());
    }

    private static String shortened(String text) {
        if (text == null || text.length() <= SHORTENED_MAX) {
            return text;
        }
        text = text.substring(0, SHORTENED_MAX);
        String candidate = text.replaceFirst("\\S+$", "");
        if (candidate.length() < SHORTENED_MIN) {
            candidate = text;
        }

        return candidate + "...";
    }

    private static Slugify initSlug() {
        return new Slugify();
    }

    @Override
    public Log getLog(long id) {
        return srv.getLog(id);
    }

    @Override
    public List<Log> getLogs() {
        return srv.getLogs();
    }

    @Override
    public Log createLog(InputStream raw) {
        return srv.createLog(raw);
    }

}
