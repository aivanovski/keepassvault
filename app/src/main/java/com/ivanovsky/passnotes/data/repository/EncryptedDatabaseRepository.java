package com.ivanovsky.passnotes.data.repository;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;

public interface EncryptedDatabaseRepository {

    boolean isOpened();

    EncryptedDatabase getDatabase();

    NoteRepository getNoteRepository();

    GroupRepository getGroupRepository();

    TemplateRepository getTemplateRepository();

    @NonNull
    OperationResult<EncryptedDatabase> open(EncryptedDatabaseKey key, FileDescriptor file);

    @NonNull
    OperationResult<Boolean> createNew(EncryptedDatabaseKey key, FileDescriptor file);

    @NonNull
    OperationResult<Boolean> close();
}
