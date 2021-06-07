/*
 * Copyright 2021 Redsaz <redsaz@gmail.com>.
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
package com.redsaz.lognition.quarkus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import static io.quarkus.jackson.ObjectMapperCustomizer.MINIMUM_PRIORITY;
import javax.inject.Singleton;

/**
 * Configures the Quarkus Jackson ObjectMapper.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
@Singleton
public class LognitionObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public int priority() {
        return MINIMUM_PRIORITY;
    }

    @Override
    public void customize(ObjectMapper objectMapper) {
        // Only include fields if they are non-null values.
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
