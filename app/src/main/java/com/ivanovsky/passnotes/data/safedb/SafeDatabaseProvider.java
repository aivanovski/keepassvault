package com.ivanovsky.passnotes.data.safedb;

import android.arch.persistence.room.Room;
import android.content.Context;

import com.ivanovsky.passnotes.data.repository.NotepadRepository;
import com.ivanovsky.passnotes.util.FileUtils;

import java.io.File;

import io.reactivex.Single;

public class SafeDatabaseProvider {

	private Context context;
	private volatile String dbName;
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
			closeDatabase();

			dbName = name;

			db = Room.databaseBuilder(context.getApplicationContext(), SafeDatabase.class, name)
					.build();
			db.getNotepadDao().getAll();
		}

		return db;
	}

	public Single<SafeDatabase> openDatabaseAsync(String name) {
		return Single.fromCallable(() -> openDatabase(name));
	}

	private SafeDatabase buildDatabase(String name) {
		return Room.databaseBuilder(context.getApplicationContext(), SafeDatabase.class, name)
				.build();
	}

	public void closeDatabase() {
		synchronized (lock) {
			if (db != null) {
				db.close();

				db = null;
				dbName = null;
				clearAllRepositories();
			}
		}
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

	public String getOpenedDBName() {
		return dbName;
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
