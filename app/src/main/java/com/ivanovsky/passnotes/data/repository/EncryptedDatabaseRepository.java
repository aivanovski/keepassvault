package com.ivanovsky.passnotes.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;
import com.ivanovsky.passnotes.data.repository.file.FSOptions;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassImplementation;

import kotlin.Unit;

public interface EncryptedDatabaseRepository {

    boolean isOpened();

    @Nullable
    EncryptedDatabase getDatabase();

    @NonNull
    OperationResult<EncryptedDatabase> open(
            @NonNull KeepassImplementation type,
            @NonNull EncryptedDatabaseKey key,
            @NonNull FileDescriptor file,
            @NonNull FSOptions options);

    @NonNull
    OperationResult<Boolean> createNew(
            @NonNull KeepassImplementation type,
            @NonNull EncryptedDatabaseKey key,
            @NonNull FileDescriptor file,
            boolean addTemplates);

    @NonNull
    OperationResult<Boolean> reload();

    @NonNull
    OperationResult<Boolean> close();

    @NonNull
    OperationResult<Unit> canOpen(
            @NonNull KeepassImplementation type,
            @NonNull EncryptedDatabaseKey key,
            @NonNull FileDescriptor file);
}
