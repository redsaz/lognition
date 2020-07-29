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
package com.redsaz.lognition.api;

import com.redsaz.lognition.api.model.CodeCounts;
import com.redsaz.lognition.api.model.Histogram;
import com.redsaz.lognition.api.model.Percentiles;
import com.redsaz.lognition.api.model.Stats;
import com.redsaz.lognition.api.model.Timeseries;
import java.util.List;

/**
 * Stores and accesses statistics such as timeseries.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public interface StatsService {

    public void createSampleLabels(long logId, List<String> labels);

    public List<String> getSampleLabels(long logId);

    public Stats getAggregate(long logId, long labelId);

    public Timeseries getTimeseries(long logId, long labelId);

    public Histogram getHistogram(long logId, long labelId);

    public Percentiles getPercentiles(long logId, long labelId);

    public void createOrUpdateAggregate(long logId, long labelId, Stats aggregate);

    public void createOrUpdateTimeseries(long logId, long labelId, Timeseries timeseries);

    public void createOrUpdateHistogram(long logId, long labelId, Histogram histogram);

    public void createOrUpdatePercentiles(long logId, long labelId, Percentiles percentiles);

    public void createOrUpdateCodeCounts(long logId, long labelId, CodeCounts overallCodeCounts);
}
