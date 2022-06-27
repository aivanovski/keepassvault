package com.ivanovsky.passnotes.data.repository.encdb;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.data.repository.TemplateDao;
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus;

import java.util.concurrent.locks.ReentrantLock;

public interface EncryptedDatabase {

    @NonNull
    ReentrantLock getLock();

    @NonNull
    FileDescriptor getFile();

    @NonNull
    DatabaseStatus getStatus();

    @NonNull
    OperationResult<EncryptedDatabaseConfig> getConfig();

    @NonNull
    OperationResult<Boolean> applyConfig(@NonNull EncryptedDatabaseConfig config);

    @NonNull
    GroupRepository getGroupRepository();

    @NonNull
    NoteRepository getNoteRepository();

    @NonNull
    TemplateDao getTemplateDao();

    @NonNull
    OperationResult<Boolean> changeKey(@NonNull EncryptedDatabaseKey oldKey,
                                       @NonNull EncryptedDatabaseKey newKey);

    @NonNull
    OperationResult<Boolean> commit();
}
