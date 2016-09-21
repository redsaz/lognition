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
package com.redsaz.meterrier.services;

import com.github.slugify.Slugify;
import com.redsaz.meterrier.api.NotesService;
import com.redsaz.meterrier.api.exceptions.AppClientException;
import com.redsaz.meterrier.api.model.Note;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Does not directly store notes, but is responsible for ensuring that the notes
 * sent to and retrieved from the store are correctly formatted, sized, and
 * without malicious/errorific content.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class SanitizedNotesService implements NotesService {

    private static final Slugify SLG = initSlug();
    private static final int SHORTENED_MAX = 60;
    private static final int SHORTENED_MIN = 12;

    private final NotesService srv;

    public SanitizedNotesService(NotesService notesService) {
        srv = notesService;
    }

    @Override
    public List<Note> getNotes() {
        return sanitizeAll(srv.getNotes());
    }

    @Override
    public Note getNote(long id) {
        return sanitize(srv.getNote(id));
    }

    @Override
    public List<Note> createAll(List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            return Collections.emptyList();
        }
        return srv.createAll(sanitizeAll(notes));
    }

    @Override
    public List<Note> updateAll(List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            return Collections.emptyList();
        }
        return srv.updateAll(sanitizeAll(notes));
    }

    @Override
    public void deleteNote(long id) {
        srv.deleteNote(id);
    }

    /**
     * Sanitizes a group of notes according to the
     * {@link #sanitize(com.redsaz.meterrier.api.model.Note)} method.
     *
     * @param notes The notes to sanitize
     * @return A List of new note instances with sanitized data.
     */
    private static List<Note> sanitizeAll(List<Note> notes) {
        List<Note> sanitizeds = new ArrayList<>(notes.size());
        for (Note note : notes) {
            sanitizeds.add(sanitize(note));
        }
        return sanitizeds;
    }

    /**
     * A note must have at least a uri, a title, and/or a body. If none of them
     * are present then note cannot be sanitized. The ID will remain unchanged.
     *
     * @param note The note to sanitize
     * @return A new note instance with sanitized data.
     */
    private static Note sanitize(Note note) {
        String uriName = note.getUriName();
        if (uriName == null || uriName.isEmpty()) {
            uriName = note.getTitle();
            if (uriName == null || uriName.isEmpty()) {
                uriName = shortened(note.getBody());
                if (uriName == null || uriName.isEmpty()) {
                    throw new AppClientException("Note must have at least a uri, title, or body.");
                }
            }
        }
        uriName = SLG.slugify(uriName);

        String title = note.getTitle();
        if (title == null) {
            title = shortened(note.getBody());
            if (title == null) {
                title = "";
            }
        }
        String body = note.getBody();
        if (body == null) {
            body = "";
        }

        return new Note(note.getId(), uriName, title, body);
    }

    private static String shortened(String text) {
        if (text == null || text.length() <= SHORTENED_MAX) {
            return text;
        }
        text = text.substring(0, SHORTENED_MAX);
        String candidate = text.replaceFirst("\\S+$", "");
        if (candidate.length() < SHORTENED_MIN) {
            candidate = text;
        }

        return candidate + "...";
    }

    private static Slugify initSlug() {
        return new Slugify();
    }

}
