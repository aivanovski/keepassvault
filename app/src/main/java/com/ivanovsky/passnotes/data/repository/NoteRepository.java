package com.ivanovsky.passnotes.data.repository;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.Note;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.util.List;
import java.util.UUID;

public interface NoteRepository {

    @NonNull
    OperationResult<List<Note>> getNotesByGroupUid(UUID groupUid);

    @NonNull
    OperationResult<Integer> getNoteCountByGroupUid(UUID groupUid);

    @NonNull
    OperationResult<UUID> insert(Note note);

    @NonNull
    OperationResult<Note> getNoteByUid(UUID uid);

    @NonNull
    OperationResult<UUID> update(Note note);

    @NonNull
    OperationResult<Boolean> remove(UUID noteUid);

    @NonNull
    OperationResult<List<Note>> find(@NonNull String query);
}
