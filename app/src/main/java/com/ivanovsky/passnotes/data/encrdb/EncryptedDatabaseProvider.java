package com.ivanovsky.passnotes.data.encrdb;

import android.arch.persistence.room.Room;
import android.content.Context;

import com.ivanovsky.passnotes.data.encrdb.dao.NotepadDao;
import com.ivanovsky.passnotes.util.FileUtils;

import java.io.File;

public class EncryptedDatabaseProvider {

	private Context context;
	private volatile EncryptedDatabase database;
	private final Object lock;

	public EncryptedDatabaseProvider(Context context) {
		this.context = context;
		this.lock = new Object();
	}

	public boolean isDatabaseExist(String name) {
		boolean result;

		File dbFile = getDatabaseFile(name);
		result = (dbFile != null && dbFile.exists());

		return result;
	}

	public EncryptedDatabase openDatabase(String name) {
		synchronized (lock) {
			database = Room.databaseBuilder(context.getApplicationContext(),
					EncryptedDatabase.class, name).build();
			database.getNotepadDao().getAll();
		}
		return database;
	}

	public File getDatabaseFile(String name) {
		File result = null;

		File databaseDir = FileUtils.getDatabaseDir(context);
		if (databaseDir != null) {
			result = new File(databaseDir.getPath() + File.separator + name);
		}

		return result;
	}

	public boolean isDatabaseOpened() {
		return database != null;
	}

	public NotepadDao getNotepadDao() {
		return database.getNotepadDao();
	}
}
