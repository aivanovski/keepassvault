package com.ivanovsky.passnotes.data.safedb;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.ivanovsky.passnotes.data.safedb.dao.NotepadDao;
import com.ivanovsky.passnotes.data.safedb.model.Notepad;

@Database(entities = { Notepad.class }, version = 1)
public abstract class SafeDatabase extends RoomDatabase {

	public abstract NotepadDao getNotepadDao();
}
