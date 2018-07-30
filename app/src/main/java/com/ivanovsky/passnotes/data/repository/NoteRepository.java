package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.entity.Note;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.util.List;
import java.util.UUID;

public interface NoteRepository {

	OperationResult<List<Note>> getNotesByGroupUid(UUID groupUid);
	OperationResult<Integer> getNoteCountByGroupUid(UUID groupUid);
	OperationResult<UUID> insert(Note note);
}
