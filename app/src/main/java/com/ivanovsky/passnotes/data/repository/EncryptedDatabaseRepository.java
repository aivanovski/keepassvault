package com.ivanovsky.passnotes.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;
import com.ivanovsky.passnotes.data.repository.file.FSOptions;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassImplementation;

public interface EncryptedDatabaseRepository {

    boolean isOpened();

    @Nullable
    EncryptedDatabase getDatabase();

    /**
     * Reads database from provided file and saves it as current opened database
     *
     * @param type    the implementation of Keepass
     * @param key     the key to decrypt database
     * @param file    file to read database from
     * @param options options for reading file
     * @return database
     */
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

    /**
     * Reads database from provided file and returns the result
     *
     * @param type the implementation of Keepass
     * @param key  the key to decrypt database
     * @param file file to read database from
     * @return database
     */
    @NonNull
    OperationResult<EncryptedDatabase> read(
            @NonNull KeepassImplementation type,
            @NonNull EncryptedDatabaseKey key,
            @NonNull FileDescriptor file);
}
