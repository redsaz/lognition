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
package com.redsaz.lognition.services;

import com.redsaz.lognition.api.ImportService;
import com.redsaz.lognition.api.model.ImportInfo;
import com.redsaz.lognition.api.model.Log;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does not directly store logs, but is responsible for ensuring that the logs and metadata sent to
 * and retrieved from the store are correctly formatted, sized, and without malicious/errorific
 * content.
 *
 * Default values for jtl files:
 * timestamp,elapsed,label,responseCode,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class SanitizerImportService implements ImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SanitizerImportService.class);

    private static final int SHORTENED_MAX = 512;
    private static final int SHORTENED_MIN = 12;
    private static final ThreadLocal<SimpleDateFormat> LONG_DATE
            = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
        }
    };

    private final ImportService srv;

    public SanitizerImportService(ImportService wrappee) {
        srv = wrappee;
    }

    @Override
    public void delete(long id) {
        srv.delete(id);
    }

    @Override
    public ImportInfo get(long id) {
        return srv.get(id);
    }

    @Override
    public List<ImportInfo> list() {
        return srv.list();
    }

    @Override
    public ImportInfo upload(InputStream raw, Log log, String importedFilename, long uploadedUtcMillis) {
        importedFilename = sanitizeFilename(importedFilename);
        return srv.upload(raw, log, importedFilename, uploadedUtcMillis);
    }

    @Override
    public ImportInfo update(ImportInfo source) {
        source = sanitize(source);
        return srv.update(source);
    }

    /**
     * @param source The item to sanitize
     * @return A new brief instance with sanitized data.
     */
    private static ImportInfo sanitize(ImportInfo source) {
        if (source == null) {
            source = new ImportInfo(0, null, System.currentTimeMillis());
        }
        return new ImportInfo(source.getId(), source.getImportedFilename(), source.getUploadedUtcMillis());
    }

    private static String sanitizeFilename(String original) {
        if (original == null) {
            return null;
        }
        original = original.trim();
        original = original.replaceAll("[!&;*?<>`\"']", "");
        return original;
    }

}
