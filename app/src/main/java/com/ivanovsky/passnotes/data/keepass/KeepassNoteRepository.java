package com.ivanovsky.passnotes.data.keepass;

import com.ivanovsky.passnotes.data.safedb.NoteRepository;
import com.ivanovsky.passnotes.data.safedb.dao.NoteDao;
import com.ivanovsky.passnotes.data.safedb.model.Note;

import java.util.List;
import java.util.UUID;

import io.reactivex.Single;

public class KeepassNoteRepository implements NoteRepository {

	private final NoteDao dao;

	KeepassNoteRepository(NoteDao dao) {
		this.dao = dao;
	}

	@Override
	public Single<List<Note>> getNotesByGroupUid(UUID groupUid) {
		return Single.fromCallable(() -> dao.getNotesByGroupUid(groupUid));
	}
}
