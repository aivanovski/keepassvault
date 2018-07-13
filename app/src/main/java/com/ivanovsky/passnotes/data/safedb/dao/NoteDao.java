package com.ivanovsky.passnotes.data.safedb.dao;

import com.ivanovsky.passnotes.data.safedb.model.Note;

import java.util.List;
import java.util.UUID;

public interface NoteDao {

	List<Note> getByGroupUid(UUID groupUid);
	UUID insert(Note note);
}
