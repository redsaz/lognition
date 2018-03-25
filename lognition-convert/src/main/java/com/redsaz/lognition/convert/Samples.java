/*
 * Copyright 2017 Redsaz <redsaz@gmail.com>.
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
package com.redsaz.lognition.convert;

import com.redsaz.lognition.api.model.Sample;
import java.util.List;

/**
 * Essentially a List with some extra methods.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public interface Samples {

    long getEarliestMillis();

    Sample getEarliestSample();

    List<String> getLabels();

    long getLatestMillis();

    Sample getLatestSample();

    List<Sample> getSamples();

    StatusCodeLookup getStatusCodeLookup();

    List<String> getThreadNames();

}
