package com.ivanovsky.passnotes.data.safedb.dao;

import com.ivanovsky.passnotes.data.safedb.model.Note;

import java.util.List;
import java.util.UUID;

public interface NoteDao {

	List<Note> getNotesByGroupUid(UUID groupUid);
}
