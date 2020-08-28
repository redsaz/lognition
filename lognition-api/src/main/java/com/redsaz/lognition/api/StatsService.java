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
import java.util.Map;

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

    /**
     * Retrieves a specific code count for a given log, label, and spansize.
     *
     * @param logId The log identifier
     * @param labelId The sample label identifier
     * @param spanMillis The spansize of the code counts
     * @return The code counts if found, or null otherwise.
     */
    public CodeCounts getCodeCounts(long logId, long labelId, long spanMillis);

    /**
     * Retrieves code counts of all labels for a given log and spansize.
     *
     * @param logId The log identifier
     * @param spanMillis The spansize of the code counts
     * @return If found, a map of the code counts, with the key being the sample label id and the
     * value being the code counts. If not found, an empty map is returned.
     */
    public Map<Long, CodeCounts> getCodeCountsForLog(long logId, long spanMillis);

    public void createOrUpdateAggregate(long logId, long labelId, Stats aggregate);

    public void createOrUpdateTimeseries(long logId, long labelId, Timeseries timeseries);

    public void createOrUpdateHistogram(long logId, long labelId, Histogram histogram);

    public void createOrUpdatePercentiles(long logId, long labelId, Percentiles percentiles);

    /**
     * Stores or updates the code counts for a given log, label, and spansize. The spansize is
     * looked up in the given codeCounts parameter. A spansize of 0 defines the aggregate code count
     * whereas any other positive number defines the timespan that is chunked up.
     *
     * @param logId The log identifier
     * @param labelId The sample label identifier
     * @param codeCounts The count of status codes, including spansize.
     */
    public void createOrUpdateCodeCounts(long logId, long labelId, CodeCounts codeCounts);
}
