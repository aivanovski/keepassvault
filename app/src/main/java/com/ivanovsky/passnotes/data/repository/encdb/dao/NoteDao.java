package com.ivanovsky.passnotes.data.repository.encdb.dao;

import androidx.annotation.NonNull;
import com.ivanovsky.passnotes.data.entity.Note;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher;
import java.util.List;
import java.util.UUID;

public interface NoteDao {

    @NonNull
    OperationResult<List<Note>> getAll();

    @NonNull
    OperationResult<List<Note>> getNotesByGroupUid(@NonNull UUID groupUid);

    @NonNull
    OperationResult<Note> getNoteByUid(@NonNull UUID noteUid);

    @NonNull
    OperationResult<UUID> insert(@NonNull Note note);

    @NonNull
    OperationResult<Boolean> insert(@NonNull List<Note> notes);

    // TODO: this method is used only by TemplateDaoImpl
    @NonNull
    OperationResult<Boolean> insert(@NonNull List<Note> notes, boolean doCommit);

    @NonNull
    OperationResult<UUID> update(@NonNull Note note);

    @NonNull
    OperationResult<Boolean> remove(@NonNull UUID noteUid);

    @NonNull
    OperationResult<List<Note>> find(@NonNull String query);

    @NonNull
    ContentWatcher<Note> getContentWatcher();
}
