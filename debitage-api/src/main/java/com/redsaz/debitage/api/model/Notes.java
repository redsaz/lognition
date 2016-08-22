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
package com.redsaz.debitage.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A collection of notes.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class Notes {

    private List<Note> notes;

    @JsonCreator
    public Notes(@JsonProperty("notes") List<Note> inNotes) {
        notes = Collections.unmodifiableList(new ArrayList<>(inNotes));
    }

    public List<Note> getNotes() {
        return notes;
    }
}
