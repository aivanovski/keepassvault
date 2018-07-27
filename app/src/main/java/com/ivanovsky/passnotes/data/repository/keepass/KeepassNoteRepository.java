package com.ivanovsky.passnotes.data.repository.keepass;

import com.ivanovsky.passnotes.data.entity.OperationResult;
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
	public OperationResult<List<Note>> getNotesByGroupUid(UUID groupUid) {
		return dao.getNotesByGroupUid(groupUid);
	}

	@Override
	public OperationResult<Integer> getNoteCountByGroupUid(UUID groupUid) {
		OperationResult<Integer> operation;

		OperationResult<List<Note>> notesOperation = dao.getNotesByGroupUid(groupUid);
		if (notesOperation.getResult() != null) {
			operation = OperationResult.success(notesOperation.getResult().size());
		} else {
			operation = OperationResult.error(notesOperation.getError());
		}

		return operation;
	}
}
