package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.entity.Note;

import java.util.List;
import java.util.UUID;

import io.reactivex.Single;

public interface NoteRepository {

	Single<List<Note>> getNotesByGroupUid(UUID groupUid);
}
