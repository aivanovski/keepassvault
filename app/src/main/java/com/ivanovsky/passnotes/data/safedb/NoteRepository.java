package com.ivanovsky.passnotes.data.safedb;

import com.ivanovsky.passnotes.data.safedb.model.Note;

import java.util.List;
import java.util.UUID;

import io.reactivex.Single;

public interface NoteRepository {

	Single<List<Note>> getNotesByGroupUid(UUID groupUid);
}
