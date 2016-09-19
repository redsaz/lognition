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
package com.redsaz.meterrier.api;

import com.redsaz.meterrier.api.model.Note;
import java.util.List;

/**
 * Stores and accesses notes.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public interface NotesService {

    List<Note> getNotes();

    public Note getNote(long id);

    public List<Note> createAll(List<Note> notes);

    public List<Note> updateAll(List<Note> notes);

    public void deleteNote(long id);
}
