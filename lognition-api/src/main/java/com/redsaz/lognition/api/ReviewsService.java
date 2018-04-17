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
package com.redsaz.lognition.api;

import com.redsaz.lognition.api.model.Log;
import com.redsaz.lognition.api.model.Review;
import java.util.Collection;
import java.util.List;

/**
 * Stores and accesses {@link Review}s.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public interface ReviewsService {

    public Review create(Review source);

    public Review get(long id);

    public List<Review> list();

    public Review update(Review source);

    public void delete(long id);

    public void setReviewLogs(long reviewId, Collection<Long> logIds);

    public List<Log> getReviewLogs(long reviewId);
}
