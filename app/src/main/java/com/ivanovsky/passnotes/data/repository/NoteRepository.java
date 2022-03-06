package com.ivanovsky.passnotes.data.repository;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.Note;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.util.List;
import java.util.UUID;

public interface NoteRepository {

    @NonNull
    OperationResult<List<Note>> getNotesByGroupUid(@NonNull UUID groupUid);

    @NonNull
    OperationResult<Integer> getNoteCountByGroupUid(@NonNull UUID groupUid);

    @NonNull
    OperationResult<UUID> insert(@NonNull Note note);

    @NonNull
    OperationResult<Note> getNoteByUid(@NonNull UUID uid);

    @NonNull
    OperationResult<UUID> update(@NonNull Note note);

    @NonNull
    OperationResult<Boolean> remove(@NonNull UUID noteUid);

    @NonNull
    OperationResult<List<Note>> find(@NonNull String query);
}
