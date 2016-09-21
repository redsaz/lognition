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
package com.redsaz.meterrier.store;

import com.redsaz.meterrier.api.NotesService;
import com.redsaz.meterrier.api.exceptions.AppException;
import com.redsaz.meterrier.api.exceptions.AppServerException;
import com.redsaz.meterrier.api.exceptions.NotFoundException;
import com.redsaz.meterrier.api.model.Note;
import static com.redsaz.meterrier.model.tables.Note.NOTE;
import com.redsaz.meterrier.model.tables.records.NoteRecord;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hsqldb.jdbc.JDBCPool;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep3;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Stores and accesses notes.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class HsqlNotesService implements NotesService {

    private final JDBCPool pool;

    /**
     * Create a new HSQLDB-base NotesService.
     *
     * @param jdbcPool opens connections to database
     */
    public HsqlNotesService(JDBCPool jdbcPool) {
        pool = jdbcPool;
    }

    @Override
    public List<Note> getNotes() {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);
            List<NoteRecord> nrs = context.selectFrom(NOTE).fetch();
            return recordsToNotes(nrs);
        } catch (SQLException ex) {
            throw new AppServerException("Cannot retrieve notes: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Note getNote(long id) {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

            NoteRecord nr = context.selectFrom(NOTE).where(NOTE.ID.eq(id)).fetchOne();
            return recordToNote(nr);
        } catch (SQLException ex) {
            throw new AppServerException("Cannot get note_id=" + id + " because: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Note> createAll(List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            return Collections.emptyList();
        }
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

            InsertValuesStep3<NoteRecord, String, String, String> query = context.insertInto(NOTE).columns(NOTE.URINAME, NOTE.TITLE, NOTE.BODY);
            for (Note note : notes) {
                query.values(note.getUriName(), note.getTitle(), note.getBody());
            }
            Result<NoteRecord> nrs = query.returning().fetch();
            return recordsToNotes(nrs);
        } catch (SQLException ex) {
            throw new AppServerException("Failed to create notes: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Note> updateAll(List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            return Collections.emptyList();
        }
        RuntimeException updateFailure = null;
        List<Long> ids = new ArrayList<>(notes.size());
        for (Note note : notes) {
            try (Connection c = pool.getConnection()) {
                DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

                int numNotesAffected = context.update(NOTE)
                        .set(NOTE.URINAME, note.getUriName())
                        .set(NOTE.TITLE, note.getTitle())
                        .set(NOTE.BODY, note.getBody())
                        .where(NOTE.ID.eq(note.getId())).execute();
                if (numNotesAffected != 1) {
                    throw new NotFoundException("Failed to update note_id="
                            + note.getId() + " because it does not exist.");
                }
                ids.add(note.getId());
            } catch (SQLException ex) {
                if (updateFailure == null) {
                    updateFailure = new AppException("Failed to update one or more notes.");
                }
                updateFailure.addSuppressed(ex);
            } catch (NotFoundException ex) {
                if (updateFailure == null) {
                    updateFailure = ex;
                } else {
                    updateFailure.addSuppressed(ex);
                }
            }
        }
        if (updateFailure != null) {
            throw updateFailure;
        }

        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

            Result<NoteRecord> records = context.selectFrom(NOTE).where(NOTE.ID.in(ids)).fetch();
            return recordsToNotes(records);
        } catch (SQLException ex) {
            throw new AppServerException("Sucessfully updated note_ids=" + ids
                    + " but failed to return the updated records: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteNote(long id) {
        try (Connection c = pool.getConnection()) {
            DSLContext context = DSL.using(c, SQLDialect.HSQLDB);

            context.delete(NOTE).where(NOTE.ID.eq(id)).execute();
        } catch (SQLException ex) {
            throw new AppServerException("Failed to delete note_id=" + id
                    + " because: " + ex.getMessage(), ex);
        }
    }

    private static JDBCPool initPool() {
        System.out.println("Initing DB...");
        File deciDir = new File("./meterrier");
        if (!deciDir.exists() && !deciDir.mkdirs()) {
            throw new RuntimeException("Could not create " + deciDir);
        }
        File deciDb = new File(deciDir, "meterrierdb");
        JDBCPool jdbc = new JDBCPool();
        jdbc.setUrl("jdbc:hsqldb:" + deciDb.toURI());
        jdbc.setUser("SA");
        jdbc.setPassword("SA");

        try (Connection c = jdbc.getConnection()) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
            Liquibase liquibase = new Liquibase("meterrier-db.yaml", new ClassLoaderResourceAccessor(), database);
            liquibase.update((String) null);
        } catch (SQLException | LiquibaseException ex) {
            throw new AppServerException("Cannot initialize notes service: " + ex.getMessage(), ex);
        }
        return jdbc;
    }

    private static Note recordToNote(NoteRecord nr) {
        if (nr == null) {
            return null;
        }
        return new Note(nr.getValue(NOTE.ID), nr.getValue(NOTE.URINAME), nr.getValue(NOTE.TITLE), nr.getValue(NOTE.BODY));
    }

    private static List<Note> recordsToNotes(List<NoteRecord> nrs) {
        if (nrs == null) {
            return null;
        }
        List<Note> notes = new ArrayList<>(nrs.size());
        for (NoteRecord nr : nrs) {
            Note result = recordToNote(nr);
            if (result != null) {
                notes.add(result);
            }
        }
        return notes;
    }

}
