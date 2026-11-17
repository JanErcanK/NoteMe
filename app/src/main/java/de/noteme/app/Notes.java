package de.noteme.app;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

final class Notes extends SQLiteOpenHelper {
    static final class Entry {
        final long id, created;
        final String body;
        final boolean important;
        Entry(Cursor c) {
            id = c.getLong(0); created = c.getLong(1); body = c.getString(2); important = c.getInt(3) != 0;
        }
    }
    Notes(Context c) { super(c, "notebook.db", null, 1); }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, created INTEGER NOT NULL, body TEXT NOT NULL, important INTEGER NOT NULL DEFAULT 0)");
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { throw new IllegalStateException("Unbekannte Datenbankversion"); }
    List<Entry> all(boolean oldest) {
        ArrayList<Entry> list = new ArrayList<>();
        String order = oldest ? " ASC" : " DESC";
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,created,body,important FROM notes WHERE body <> '' ORDER BY created"+order+", id"+order,null)) {
            while (c.moveToNext()) list.add(new Entry(c));
        }
        return list;
    }
    Entry get(long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,created,body,important FROM notes WHERE id=?", args(id))) {
            return c.moveToFirst() ? new Entry(c) : null;
        }
    }
    private String[] args(long id) { return new String[]{Long.toString(id)}; }
    long save(long id, String text) {
        SQLiteDatabase db = getWritableDatabase();
        if (text.trim().isEmpty()) text = "";
        if (id == 0 && text.isEmpty()) return 0;
        ContentValues values = new ContentValues();
        values.put("body",text);
        if (id != 0) {
            db.update("notes",values,"id=?",args(id));
            return id;
        }
        values.put("created",System.currentTimeMillis());
        return db.insertOrThrow("notes",null,values);
    }
    void discardEmpty(long id) {
        if (id != 0) getWritableDatabase().delete("notes","id=? AND body=''",args(id));
    }
    void mark(Collection<Long> ids, boolean important) {
        batch(ids, id -> getWritableDatabase().execSQL("UPDATE notes SET important=? WHERE id=?",new Object[]{important ? 1 : 0,id}));
    }
    void delete(Collection<Long> ids) {
        batch(ids, id -> getWritableDatabase().delete("notes","id=?",args(id)));
    }
    private void batch(Collection<Long> ids, java.util.function.Consumer<Long> action) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try { for(long id : ids) action.accept(id); db.setTransactionSuccessful(); }
        finally { db.endTransaction(); }
    }
    void merge(Set<Long> ids) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ArrayList<Entry> entries = new ArrayList<>();
            for (Entry e : all(true)) if (ids.contains(e.id)) entries.add(e);
            if (entries.size() < 2) return;
            StringJoiner joined = new StringJoiner("\n\n");
            boolean starred = false;
            for (Entry e : entries) { joined.add(e.body); starred |= e.important; }
            long keep = entries.get(0).id;
            save(keep,joined.toString());
            ContentValues flag = new ContentValues();
            flag.put("important",starred);
            db.update("notes",flag,"id=?",args(keep));
            for (Entry e : entries) if (e.id != keep) db.delete("notes","id=?",args(e.id));
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
}

