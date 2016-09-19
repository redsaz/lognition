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
package com.redsaz.meterrier.view;

import com.redsaz.meterrier.api.MeterrierMediaType;
import com.redsaz.meterrier.api.NotesService;
import com.redsaz.meterrier.api.model.Note;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * An endpoint for accessing notes. Many of the REST endpoints and browser
 * endpoints are identical where possible; look at docs/endpoints.md for why.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
@Path("/notes")
public class NotesResource {

    private NotesService notesSrv;

    public NotesResource() {
    }

    @Inject
    public NotesResource(NotesService notesService) {
        notesSrv = notesService;
    }

    /**
     * Lists all of the notes URI and titles.
     *
     * @return Notes, by URI and title.
     */
    @GET
    @Produces(MeterrierMediaType.NOTES_V1_JSON)
    public Response listNotes() {
        return Response.ok(notesSrv.getNotes()).build();
    }

    /**
     * Get the note contents.
     *
     * @param id The id of the note.
     * @param uriName The uri title of the note, not used in retrieving the data
     * but helpful for users which may read the note.
     * @return Note.
     */
    @GET
    @Produces({MeterrierMediaType.NOTE_V1_JSON})
    @Path("{id}/{uriName}")
    public Response getNote(@PathParam("id") long id, @PathParam("uriName") String uriName) {
        Note note = notesSrv.getNote(id);
        if (note == null) {
            throw new NotFoundException("Could not find note id=" + id);
        }
        return Response.ok(note).build();
    }

    /**
     * Get the note contents.
     *
     * @param id The id of the note.
     * @return Note.
     */
    @GET
    @Produces({MeterrierMediaType.NOTE_V1_JSON})
    @Path("{id}")
    public Response getNoteById(@PathParam("id") long id) {
        Note note = notesSrv.getNote(id);
        if (note == null) {
            throw new NotFoundException("Could not find note id=" + id);
        }
        return Response.ok(note).build();
    }

    @POST
    @Consumes(MeterrierMediaType.NOTES_V1_JSON)
    @Produces({MeterrierMediaType.NOTES_V1_JSON})
    public Response createNotes(List<Note> notes) {
        return Response.status(Status.CREATED).entity(notesSrv.createAll(notes)).build();
    }

    @POST
    @Consumes(MeterrierMediaType.NOTE_V1_JSON)
    @Produces({MeterrierMediaType.NOTE_V1_JSON})
    public Response createNote(Note note) {
        List<Note> notes = Collections.singletonList(note);
        return Response.status(Status.CREATED).entity(notesSrv.createAll(notes)).build();
    }

    @PUT
    @Consumes(MeterrierMediaType.NOTES_V1_JSON)
    @Produces({MeterrierMediaType.NOTES_V1_JSON})
    public Response updateNotes(List<Note> notes) {
        return Response.status(Status.ACCEPTED).entity(notesSrv.updateAll(notes)).build();
    }

    @PUT
    @Consumes(MeterrierMediaType.NOTE_V1_JSON)
    @Produces({MeterrierMediaType.NOTE_V1_JSON})
    @Path("{id}")
    public Response updateNote(@PathParam("id") long id, Note note) {
        Note withId = new Note(id, note.getUriName(), note.getTitle(), note.getBody());
        List<Note> notes = Collections.singletonList(withId);
        return Response.status(Status.ACCEPTED).entity(notesSrv.updateAll(notes)).build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteNote(@PathParam("id") long id) {
        notesSrv.deleteNote(id);
        return Response.status(Status.NO_CONTENT).build();
    }

}
