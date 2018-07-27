package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.repository.db.AppDatabase;
import com.ivanovsky.passnotes.data.entity.UsedFile;

import java.util.List;

public class UsedFileRepository {

	@SuppressWarnings("unused")
	private static final String TAG = UsedFileRepository.class.getSimpleName();

	private final AppDatabase db;

	public UsedFileRepository(AppDatabase db) {
		this.db = db;
	}

	public List<UsedFile> getAllUsedFiles() {
		return db.getUsedFileDao().getAll();
	}

	public void insert(UsedFile file) {
		long id = db.getUsedFileDao().insert(file);
		file.setId((int) id);
	}
}
