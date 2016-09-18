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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.redsaz.debitage.api.exceptions.ExceptionMappers;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * The base entrypoint for the application, lists the classes with endpoints
 * which comprise the application.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
@ApplicationPath("/")
public class DebitageApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(DebitageResource.class);
        classes.add(NotesResource.class);
        classes.add(BrowserNotesResource.class);
        classes.add(LogsResource.class);
        classes.add(BrowserLogsResource.class);
        classes.add(JacksonJsonProvider.class);
        classes.add(StaticContentFilter.class);
        classes.add(Templater.class);
        classes.add(FreemarkerTemplater.class);
        classes.add(SanitizedNotesService.class);
        classes.add(ExceptionMappers.class);
        classes.add(ExceptionMappers.AppExceptionMapper.class);
        classes.add(ExceptionMappers.NotFoundMapper.class);
        return classes;
    }

}
