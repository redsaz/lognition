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
package com.redsaz.meterrier;

import com.redsaz.meterrier.api.exceptions.AppClientException;
import com.redsaz.meterrier.api.exceptions.AppException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Handles Not Found exceptions, specifically for HTML output.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class BasicExceptionMapper implements ExceptionMapper<AppException> {

    @Override
    public Response toResponse(AppException e) {
        ResponseBuilder resp = Response.status(500)
                .entity(e.getMessage())
                .type(MediaType.APPLICATION_JSON);
        if (e instanceof AppClientException) {
            resp.status(400);
        }
        return resp.build();
    }

}
