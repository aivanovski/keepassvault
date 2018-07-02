package com.ivanovsky.passnotes.data.keepass;

import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.data.safedb.NotepadRepository;
import com.ivanovsky.passnotes.data.safedb.dao.NotepadDao;
import com.ivanovsky.passnotes.data.safedb.model.Notepad;

import java.util.List;

import io.reactivex.Single;

public class KeepassNotepadRepository implements NotepadRepository {

	private final NotepadDao dao;
	private final Object lock;

	KeepassNotepadRepository(NotepadDao dao) {
		this.dao = dao;
		this.lock = new Object();
	}

	@Override
	public Single<List<Notepad>> getAllNotepads() {
		return Single.fromCallable(dao::getAll);
	}

	@Override
	public boolean isTitleFree(String title) {
		boolean result;

		synchronized (lock) {
			result = !Stream.of(dao.getAll())
					.anyMatch(notepad -> title.equals(notepad.getTitle()));
		}

		return result;
	}

	@Override
	public void insert(Notepad notepad) {
		synchronized (lock) {
			String uid = dao.insert(notepad);
			notepad.setUid(uid);
		}
	}
}
