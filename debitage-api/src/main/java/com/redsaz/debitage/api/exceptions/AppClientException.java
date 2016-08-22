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
package com.redsaz.debitage.api.exceptions;

/**
 * The base exception type for exceptions from this application due to some
 * action on the client's part.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class AppClientException extends AppException {

    public AppClientException() {
        super();
    }

    public AppClientException(String message) {
        super(message);
    }

    public AppClientException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public AppClientException(Throwable throwable) {
        super(throwable);
    }
}
