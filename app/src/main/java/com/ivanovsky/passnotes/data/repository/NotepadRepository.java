package com.ivanovsky.passnotes.data.repository;

import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.data.safedb.SafeDatabase;
import com.ivanovsky.passnotes.data.safedb.dao.NotepadDao;
import com.ivanovsky.passnotes.data.safedb.model.Notepad;

import java.util.List;

import io.reactivex.Single;

public class NotepadRepository {

	private final NotepadDao notepadDao;
	private final Object lock;

	public NotepadRepository(SafeDatabase db) {
		this.notepadDao = db.getNotepadDao();
		this.lock = new Object();
	}

	public Single<List<Notepad>> getAllNotepads() {
		return Single.fromCallable(notepadDao::getAll);
	}

	public boolean isTitleFree(String title) {
		boolean result;

		synchronized (lock) {
			result = !Stream.of(notepadDao.getAll())
					.anyMatch(notepad -> title.equals(notepad.getTitle()));
		}

		return result;
	}

	public void insert(Notepad notepad) {
		synchronized (lock) {
			long id = notepadDao.insert(notepad);
			notepad.setId((int) id);
		}
	}
}
