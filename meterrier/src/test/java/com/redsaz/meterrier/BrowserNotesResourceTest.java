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
import com.redsaz.meterrier.api.model.Note;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import static org.mockito.Mockito.when;
import org.testng.annotations.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

/**
 * @author Redsaz <redsaz@gmail.com>
 */
public class BrowserNotesResourceTest extends BaseResourceTest {

    @Test(dataProvider = DEFAULT_DP)
    public void testListNotes(Context context) throws URISyntaxException {
        // Given that the service is running...
        // ... When the user views the notes page...
        MockHttpRequest request = MockHttpRequest.get("/notes").accept(MediaType.TEXT_HTML);
        HttpResponse response = context.invoke(request);

        // ... Then the notes list page should be returned.
        assertEquals(response.getStatus(), HttpServletResponse.SC_OK);

        verify(context.notesService).getNotes();
        verify(context.templater).buildFromTemplate(any(), any(String.class));
    }

    @Test(dataProvider = DEFAULT_DP)
    public void testEditNote(Context context) throws URISyntaxException {
        // Given that a note with id=1 exists...
        // ... when the user requests the note edit page...
        MockHttpRequest request = MockHttpRequest.get("/notes/1/edit").accept(MediaType.TEXT_HTML);
        HttpResponse response = context.invoke(request);

        // ... Then the page should be retrieved, and the notes contents accessed.
        assertEquals(response.getStatus(), HttpServletResponse.SC_OK);

        verify(context.notesService).getNote(1L);
        verify(context.templater).buildFromTemplate(any(), any(String.class));
    }

    @Test(dataProvider = DEFAULT_DP)
    public void testEditNoteNotFound(Context context) throws URISyntaxException {
        // Given there is not a note with id=0...
        // ...when the note id=0 edit page is requested...
        MockHttpRequest request = MockHttpRequest.get("/notes/0/edit").accept(MediaType.TEXT_HTML);
        HttpResponse response = context.invoke(request);

        // ...then a 404 should be returned.
        assertEquals(response.getStatus(), HttpServletResponse.SC_NOT_FOUND);

        verify(context.notesService).getNote(0L);
    }

    @Test(dataProvider = DEFAULT_DP)
    public void testGetNote(Context context) throws URISyntaxException {
        // Given that a note with id=1 exists...
        // ... when the user requests the note page...
        MockHttpRequest request = MockHttpRequest.get("/notes/1").accept(MediaType.TEXT_HTML);
        HttpResponse response = context.invoke(request);

        // ... Then the page should be retrieved, and the notes contents accessed.
        assertEquals(response.getStatus(), HttpServletResponse.SC_OK);

        verify(context.notesService).getNote(1L);
        verify(context.templater).buildFromTemplate(any(), any(String.class));
    }

    @Test(dataProvider = DEFAULT_DP)
    public void testGetNoteNotFound(Context context) throws URISyntaxException {
        // Given there is not a note with id=0...
        // ...when the note id=0 page is requested...
        MockHttpRequest request = MockHttpRequest.get("/notes/0").accept(MediaType.TEXT_HTML);
        HttpResponse response = context.invoke(request);

        // ...then a 404 should be returned.
        assertEquals(response.getStatus(), HttpServletResponse.SC_NOT_FOUND);

        verify(context.notesService).getNote(0L);
    }

    @Test(dataProvider = DEFAULT_DP)
    public void testCreateNote(Context context) throws URISyntaxException {
        // Given that the service is running...
        // ... when the user requests the create note page...
        MockHttpRequest request = MockHttpRequest.get("/notes/create").accept(MediaType.TEXT_HTML);
        HttpResponse response = context.invoke(request);

        // ... Then the page should be retrieved.
        assertEquals(response.getStatus(), HttpServletResponse.SC_OK);

        verify(context.templater).buildFromTemplate(any(), any(String.class));
    }

    @Test(dataProvider = DEFAULT_DP)
    public void testDeleteNote(Context context) throws URISyntaxException {
        // Given that a note with id=1 exists...
        // ... when the user requests the note deleted...
        MockHttpRequest request = MockHttpRequest
                .post("/notes/delete")
                .addFormHeader("id", "1")
                .accept(MediaType.TEXT_HTML);
        HttpResponse response = context.invoke(request);

        // ... Then the note should be deleted, and the client instructed to
        // redirect to the notes list page.
        assertEquals(response.getStatus(), HttpServletResponse.SC_SEE_OTHER);
        URI actualLocation = (URI) response.getOutputHeaders().getFirst("Location");
        URI expectedLocation = URI.create("/notes");
        assertEquals(actualLocation, expectedLocation);
        verify(context.notesService).deleteNote(1L);
    }

    @Test(dataProvider = DEFAULT_DP)
    public void testFinishEditOrCreateNote_Edit(Context context) throws URISyntaxException {
        // Given that a note with id=1 exists...
        // ... when the user requests the note edited...
        MockHttpRequest request = MockHttpRequest
                .post("/notes")
                .addFormHeader("id", "1")
                .addFormHeader("title", "Example Title")
                .addFormHeader("body", "Example Body")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.TEXT_HTML);
        when(context.notesService.updateAll(any(List.class)))
                .thenReturn(Collections.singletonList(
                        new Note(1, "example-title", "Example Title", "Example Body")));
        HttpResponse response = context.invoke(request);

        // ... Then the note should be edited, and the client instructed to
        // redirect to the notes list page.
        assertEquals(response.getStatus(), HttpServletResponse.SC_SEE_OTHER);
        URI actualLocation = (URI) response.getOutputHeaders().getFirst("Location");
        URI expectedLocation = URI.create("/notes");
        assertEquals(actualLocation, expectedLocation);
        verify(context.notesService).updateAll(any(List.class));
    }

    @Test(dataProvider = DEFAULT_DP)
    public void testFinishEditOrCreateNote_EditNotFound(Context context) throws URISyntaxException {
        // Given that a note with id=2 does not exist...
        // ... when the user requests the note edited...
        MockHttpRequest request = MockHttpRequest
                .post("/notes")
                .addFormHeader("id", "2")
                .addFormHeader("title", "Example Title")
                .addFormHeader("body", "Example Body")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.TEXT_HTML);
        when(context.notesService.updateAll(any(List.class)))
                .thenThrow(new NotFoundException("Failed to update one or more notes."));
        HttpResponse response = context.invoke(request);

        // ... Then not found status code should be returned.
        assertEquals(response.getStatus(), HttpServletResponse.SC_NOT_FOUND);
        verify(context.notesService).updateAll(any(List.class));
    }

    @Test(dataProvider = DEFAULT_DP)
    public void testFinishEditOrCreateNote_Create(Context context) throws URISyntaxException {
        // Given that the user is on the create note page...
        // ... when the user clicks the button to create the note...
        MockHttpRequest request = MockHttpRequest
                .post("/notes")
                .addFormHeader("id", "0")
                .addFormHeader("title", "Example Title")
                .addFormHeader("body", "Example Body")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.TEXT_HTML);
        when(context.notesService.createAll(any(List.class)))
                .thenReturn(Collections.singletonList(
                        new Note(2, "example-title", "Example Title", "Example Body")));
        HttpResponse response = context.invoke(request);

        // ... Then the note should be edited, and the client instructed to
        // redirect to the notes list page.
        assertEquals(response.getStatus(), HttpServletResponse.SC_SEE_OTHER);
        URI actualLocation = (URI) response.getOutputHeaders().getFirst("Location");
        URI expectedLocation = URI.create("/notes");
        assertEquals(actualLocation, expectedLocation);
        verify(context.notesService).createAll(any(List.class));
    }

    @Test(dataProvider = DEFAULT_DP)
    public void testFinishEditOrCreateNote_Create_NoContentError(Context context) throws URISyntaxException {
        // Given that the user is on the create note page...
        // ... when the user does not fill out any content and
        // clicks the button to create the note...
        MockHttpRequest request = MockHttpRequest
                .post("/notes")
                .addFormHeader("id", "0")
                .addFormHeader("title", "")
                .addFormHeader("body", "")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.TEXT_HTML);
        when(context.notesService.createAll(any(List.class)))
                .thenThrow(new AppClientException("Note must have at least a uri, title, or body."));
        HttpResponse response = context.invoke(request);

        // ... Then the note should not be created and the client receives an
        // error page.
        assertEquals(response.getStatus(), HttpServletResponse.SC_BAD_REQUEST);
        verify(context.notesService).createAll(any(List.class));
    }

}
