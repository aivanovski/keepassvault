package com.ivanovsky.passnotes.data.repository.keepass;

import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao;
import com.ivanovsky.passnotes.data.entity.Note;

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

	@Override
	public Integer getNoteCountByGroupUid(UUID groupUid) {
		return dao.getNotesByGroupUid(groupUid).size();
	}
}
