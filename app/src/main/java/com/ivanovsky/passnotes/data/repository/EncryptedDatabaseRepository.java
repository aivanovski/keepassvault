package com.ivanovsky.passnotes.data.repository;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;
import com.ivanovsky.passnotes.data.repository.file.FSOptions;

public interface EncryptedDatabaseRepository {

    boolean isOpened();

    EncryptedDatabase getDatabase();

    @NonNull
    OperationResult<EncryptedDatabase> open(@NonNull EncryptedDatabaseKey key,
                                            @NonNull FileDescriptor file,
                                            @NonNull FSOptions options);

    @NonNull
    OperationResult<Boolean> createNew(@NonNull EncryptedDatabaseKey key,
                                       @NonNull FileDescriptor file,
                                       boolean addTemplates);

    @NonNull
    OperationResult<Boolean> close();
}
