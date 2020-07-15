package com.ivanovsky.passnotes.data.repository.keepass;

import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao;
import com.ivanovsky.passnotes.data.entity.Note;

import java.util.List;
import java.util.UUID;

public class KeepassNoteRepository implements NoteRepository {

	private final NoteDao dao;

	KeepassNoteRepository(NoteDao dao) {
		this.dao = dao;
	}

	@Override
	public OperationResult<List<Note>> getNotesByGroupUid(UUID groupUid) {
		return dao.getNotesByGroupUid(groupUid);
	}

	@Override
	public OperationResult<Integer> getNoteCountByGroupUid(UUID groupUid) {
		OperationResult<Integer> result = new OperationResult<>();

		OperationResult<List<Note>> getNotesResult = dao.getNotesByGroupUid(groupUid);
		if (getNotesResult.getObj() != null) {
			result.setObj(getNotesResult.getObj().size());
		} else {
			result.setError(getNotesResult.getError());
		}

		return result;
	}

	@Override
	public OperationResult<UUID> insert(Note note) {
		return dao.insert(note);
	}

	@Override
	public OperationResult<Note> getNoteByUid(UUID uid) {
		return dao.getNoteByUid(uid);
	}

	@Override
	public OperationResult<UUID> update(Note note) {
		return dao.update(note);
	}

	@Override
	public OperationResult<Boolean> remove(UUID noteUid) {
		return dao.remove(noteUid);
	}
}
