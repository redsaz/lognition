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
package com.redsaz.lognition.view;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.StringWriter;
import javax.enterprise.context.ApplicationScoped;

/**
 * Holds the {@link Configuration} since it cannot be directly proxied by CDI.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
@ApplicationScoped
public class FreemarkerTemplater implements Templater {

    private Configuration cfg;

    public FreemarkerTemplater() {
        cfg = initConfig();
    }

    @Override
    public String buildFromTemplate(Object dataModel, String templateName) {
        try {
            Template temp = cfg.getTemplate("page.ftl");
            StringWriter sw = new StringWriter();
            temp.process(dataModel, sw);
            return sw.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Cannot load template: " + ex.getMessage(), ex);
        } catch (TemplateException ex) {
            throw new RuntimeException("Cannot process template: " + ex.getMessage(), ex);
        }
    }

    private static Configuration initConfig() {
        Configuration config = new Configuration(Configuration.VERSION_2_3_23);
        config.setClassForTemplateLoading(FreemarkerTemplater.class, "templates");
        config.setDefaultEncoding("UTF-8");
        // DEBUG_HANDLER is better for debug, not production
        config.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        return config;
    }
}
