package org.houxg.leamonax.database;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.Join;
import com.raizlabs.android.dbflow.sql.language.NameAlias;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.language.property.IProperty;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.Note_Table;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.model.RelationshipOfNoteTag;
import org.houxg.leamonax.model.RelationshipOfNoteTag_Table;
import org.houxg.leamonax.model.Tag;
import org.houxg.leamonax.model.Tag_Table;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NoteDataStore {
    public static List<Note> searchByTitle(String keyword) {
        keyword = String.format(Locale.US, "%%%s%%", keyword);
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.userId.eq(Account.getCurrent().getUserId()))
                .and(Note_Table.title.like(keyword))
                .and(Note_Table.isTrash.eq(false))
                .and(Note_Table.isDeleted.eq(false))
                .queryList();
    }

    public static void updateFTSNoteByLocalId(Long localId) {
        Note note = getByLocalId(localId);
        DatabaseWrapper databaseWrapper = FlowManager.getWritableDatabase(AppDataBase.class);
        ContentValues args = new ContentValues();
        args.put("content", note.getContent());
        // String query = "UPDATE fts_note SET content = '" + note.getContent() + "' where rowid = " + localId;
        databaseWrapper.updateWithOnConflict("fts_note", args, "rowid = " + localId, null, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static boolean isExistsTableFTSNote() {
        boolean result = false;
        DatabaseWrapper databaseWrapper = FlowManager.getWritableDatabase(AppDataBase.class);
        String query = "select count(*) as c from sqlite_master where type ='table' and name ='fts_note'";
        Cursor cursor = databaseWrapper.rawQuery(query, null);
        if(cursor.moveToNext()){
            int count = cursor.getInt(0);
            if(count > 0){
                result = true;
            }
        }
        return result;
    }

    public static void createTableFTSNote() {
        DatabaseWrapper databaseWrapper = FlowManager.getWritableDatabase(AppDataBase.class);
        String query = "CREATE VIRTUAL TABLE fts_note USING fts4 (content='note', content)";
        databaseWrapper.execSQL(query);
    }

    public static void FTSNoteRebuild() {
        if (!isExistsTableFTSNote()) {
            createTableFTSNote();
        }
        FTSNoteRebuildInternal();
    }

    public static void FTSNoteRebuildInternal() {
        DatabaseWrapper databaseWrapper = FlowManager.getWritableDatabase(AppDataBase.class);
        String query = "INSERT INTO fts_note(fts_note) VALUES('rebuild')";//This can be slow
        databaseWrapper.execSQL(query);
    }

    public static List<Note> searchByKeyword(String keyword) {
        if (!isExistsTableFTSNote()) {
            createTableFTSNote();
            return searchByTitle(keyword);
        } else {
            return searchByFullTextSearch(keyword);
        }

    }
    public static List<Note> searchByFullTextSearch(String keyword) {
        Set<Long> set = new LinkedHashSet<>();
        DatabaseWrapper databaseWrapper = FlowManager.getWritableDatabase(AppDataBase.class);
        String query = "select id from note where userid = ? and istrash = 0 and isdeleted = 0 and id in " +
                "(select rowid from fts_note where fts_note match ?)";////查询Content中满足条件的记录
        Cursor cursor = databaseWrapper.rawQuery(query, new String[]{Account.getCurrent().getUserId(), "*" + keyword + "*"});
        while(cursor.moveToNext()) {
            set.add(cursor.getLong(cursor.getColumnIndex("id")));
        }
        cursor.close();

        query = "select id from note where userid = ? and istrash = 0 and isdeleted = 0 and title like ?";//查询title中满足条件的记录
        cursor = databaseWrapper.rawQuery(query, new String[]{Account.getCurrent().getUserId(), "%" + keyword + "%"});//查询Content中满足条件的记录
        while(cursor.moveToNext()) {
            set.add(cursor.getLong(cursor.getColumnIndex("id")));
        }
        cursor.close();

        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.id.in(set))
                .queryList();
    }


    public static Note getByServerId(String serverId) {
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.noteId.eq(serverId))
                .querySingle();
    }

    public static Note getByLocalId(long localId) {
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.id.eq(localId))
                .querySingle();
    }

    public static List<Note> getAllNotes(String userId) {
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.userId.eq(userId))
                .and(Note_Table.isDeleted.eq(false))
                .and(Note_Table.isTrash.eq(false))
                .queryList();
    }

    public static List<Note> getAllDirtyNotes(String userId) {
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.userId.eq(userId))
                .and(Note_Table.isDeleted.eq(false))
                .and(Note_Table.isDirty.eq(true))
                .queryList();
    }

    public static List<Note> getNotesFromNotebook(String userId, long localNotebookId) {
        Notebook notebook = NotebookDataStore.getByLocalId(localNotebookId);
        if (notebook == null) {
            return new ArrayList<>();
        }
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.notebookId.eq(notebook.getNotebookId()))
                .and(Note_Table.userId.eq(userId))
                .and(Note_Table.isDeleted.eq(false))
                .and(Note_Table.isTrash.eq(false))
                .queryList();
    }

    public static List<Note> getByTagText(String tagText, String userId) {
        Tag tag = Tag.getByText(tagText, userId);
        if (tag == null) {
            return new ArrayList<>();
        }
        return getNotesByTagId(tag.getId());
    }

    private static List<Note> getNotesByTagId(long tagId) {
        IProperty[] properties = Note_Table.ALL_COLUMN_PROPERTIES;
        NameAlias nameAlias = NameAlias.builder("N").build();
        for (int i = 0; i < properties.length; i++) {
            properties[i] = properties[i].withTable(nameAlias);
        }
        return SQLite.select(properties)
                .from(Note.class).as("N")
                .join(RelationshipOfNoteTag.class, Join.JoinType.INNER).as("R")
                .on(Tag_Table.id.withTable(NameAlias.builder("N").build())
                        .eq(RelationshipOfNoteTag_Table.noteLocalId.withTable(NameAlias.builder("R").build())))
                .where(RelationshipOfNoteTag_Table.tagLocalId.withTable(NameAlias.builder("R").build()).eq(tagId))
                .queryList();
    }

    public static void deleteAll(String userId) {
        SQLite.delete()
                .from(Note.class)
                .where(Note_Table.userId.eq(userId))
                .execute();
    }
}
