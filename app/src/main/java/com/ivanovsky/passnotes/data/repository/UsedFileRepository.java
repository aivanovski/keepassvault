package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.db.AppDatabase;
import com.ivanovsky.passnotes.data.db.model.UsedFile;

import java.util.List;

import io.reactivex.Single;

public class UsedFileRepository {

	@SuppressWarnings("unused")
	private static final String TAG = UsedFileRepository.class.getSimpleName();

	private final AppDatabase db;

	public UsedFileRepository(AppDatabase db) {
		this.db = db;
	}

	public Single<List<UsedFile>> getAllUsedFiles() {
		return Single.fromCallable(() -> db.getUsedFileDao().getAll());
	}

	public void insert(UsedFile file) {
		long id = db.getUsedFileDao().insert(file);
		file.setId((int) id);
	}
}
