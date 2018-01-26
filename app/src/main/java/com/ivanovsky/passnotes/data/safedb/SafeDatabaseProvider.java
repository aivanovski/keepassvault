package com.ivanovsky.passnotes.data.safedb;

import android.arch.persistence.room.Room;
import android.content.Context;

import com.ivanovsky.passnotes.data.repository.NotepadRepository;
import com.ivanovsky.passnotes.util.FileUtils;

import java.io.File;

public class SafeDatabaseProvider {

	private Context context;
	private volatile SafeDatabase db;
	private volatile NotepadRepository notepadRepository;
	private final Object lock;

	public SafeDatabaseProvider(Context context) {
		this.context = context;
		this.lock = new Object();
	}

	public boolean isDatabaseExist(String name) {
		boolean result;

		File dbFile = getDatabaseFile(name);
		result = (dbFile != null && dbFile.exists());

		return result;
	}

	public SafeDatabase openDatabase(String name) {
		synchronized (lock) {
			db = Room.databaseBuilder(context.getApplicationContext(),
					SafeDatabase.class, name).build();
			db.getNotepadDao().getAll();

			clearAllRepositories();

		}
		return db;
	}

	private void clearAllRepositories() {
		notepadRepository = null;
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
		return db != null;
	}

	public NotepadRepository getNotepadRepository() {
		synchronized (lock) {
			if (notepadRepository == null) {
				notepadRepository = new NotepadRepository(db);
			}
		}

		return notepadRepository;
	}
}
