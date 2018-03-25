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
package com.redsaz.lognition.api.exceptions;

/**
 * The base exception type for exceptions from this application due to some
 * action on the server's part (misconfiguration, required service not found,
 * etc).
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class AppServerException extends AppException {

    public AppServerException() {
        super();
    }

    public AppServerException(String message) {
        super(message);
    }

    public AppServerException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public AppServerException(Throwable throwable) {
        super(throwable);
    }
}
