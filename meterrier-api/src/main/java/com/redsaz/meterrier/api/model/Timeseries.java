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
package com.redsaz.meterrier.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A series of {@link Stats} over a length of time.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class Timeseries {

    private final long spanMillis;
    private final List<Stats> statsList;

    @JsonCreator
    public Timeseries(
            @JsonProperty("spanMillis") long spanMillis,
            @JsonProperty("statsList") Collection<Stats> statsList
    ) {
        this.spanMillis = spanMillis;
        if (statsList == null) {
            this.statsList = null;
        } else {
            this.statsList = Collections.unmodifiableList(new ArrayList<Stats>(statsList));
        }
    }

    public List<Stats> getStatsList() {
        return statsList;
    }

    public long getSpanMillis() {
        return spanMillis;
    }

}
