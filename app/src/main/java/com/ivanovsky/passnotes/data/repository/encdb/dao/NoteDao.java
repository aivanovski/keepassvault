package com.ivanovsky.passnotes.data.repository.encdb.dao;

import com.ivanovsky.passnotes.data.entity.Note;

import java.util.List;
import java.util.UUID;

public interface NoteDao {

	List<Note> getNotesByGroupUid(UUID groupUid);
}
