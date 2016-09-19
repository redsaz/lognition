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
package com.redsaz.debitage.api;

import com.redsaz.debitage.api.model.Log;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Stores and accesses logs/measurements.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public interface LogsService {

//    List<LogBrief> getLogBriefs();
//    LogBrief getLogBrief(long id);
    public OutputStream getLogContent(long id);

    public Log getLog(long id);

    public List<Log> getLogs();

//    public LogBrief createLog(LogBrief source);
    public Log createLog(InputStream raw);

    public void deleteLog(long id);
}
