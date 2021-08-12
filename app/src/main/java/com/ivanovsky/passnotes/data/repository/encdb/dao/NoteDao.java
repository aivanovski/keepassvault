package com.ivanovsky.passnotes.data.repository.encdb.dao;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.Note;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.util.List;
import java.util.UUID;

public interface NoteDao {

    @NonNull
    OperationResult<List<Note>> getNotesByGroupUid(UUID groupUid);

    @NonNull
    OperationResult<Note> getNoteByUid(UUID noteUid);

    @NonNull
    OperationResult<UUID> insert(Note note);

    @NonNull
    OperationResult<Boolean> insert(List<Note> notes);

    @NonNull
    OperationResult<UUID> update(Note note);

    @NonNull
    OperationResult<Boolean> remove(UUID noteUid);
}
