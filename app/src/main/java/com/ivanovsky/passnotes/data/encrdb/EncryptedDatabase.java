package com.ivanovsky.passnotes.data.encrdb;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.ivanovsky.passnotes.data.encrdb.dao.NotepadDao;
import com.ivanovsky.passnotes.data.encrdb.model.Notepad;

@Database(entities = { Notepad.class }, version = 1)
public abstract class EncryptedDatabase extends RoomDatabase {

	public abstract NotepadDao getNotepadDao();
}
