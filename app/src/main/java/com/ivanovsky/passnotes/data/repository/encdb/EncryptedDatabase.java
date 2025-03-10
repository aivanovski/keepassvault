package com.ivanovsky.passnotes.data.repository.encdb;

import androidx.annotation.NonNull;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.TemplateDao;
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao;
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao;
import com.ivanovsky.passnotes.data.repository.file.FSOptions;
import java.util.concurrent.locks.ReentrantLock;

public interface EncryptedDatabase {

    @NonNull
    ReentrantLock getLock();

    @NonNull
    FileDescriptor getFile();

    @NonNull
    EncryptedDatabaseKey getKey();

    @NonNull
    FSOptions getFSOptions();

    @NonNull
    OperationResult<EncryptedDatabaseConfig> getConfig();

    @NonNull
    OperationResult<Boolean> applyConfig(@NonNull EncryptedDatabaseConfig config);

    @NonNull
    GroupDao getGroupDao();

    @NonNull
    NoteDao getNoteDao();

    @NonNull
    TemplateDao getTemplateDao();

    // TODO: refactor, change key should not invoke commit
    @NonNull
    OperationResult<Boolean> changeKey(
            @NonNull EncryptedDatabaseKey oldKey, @NonNull EncryptedDatabaseKey newKey);

    @NonNull
    OperationResult<Boolean> commit();

    @NonNull
    OperationResult<Boolean> commitTo(@NonNull FileDescriptor output, @NonNull FSOptions fsOptions);

    @NonNull
    DatabaseWatcher getWatcher();
}
