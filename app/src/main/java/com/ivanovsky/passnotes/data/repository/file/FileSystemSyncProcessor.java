package com.ivanovsky.passnotes.data.repository.file;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.SyncStatus;

import java.util.List;

public interface FileSystemSyncProcessor {

    List<FileDescriptor> getLocallyModifiedFiles();

    @NonNull
    SyncStatus getSyncStatusForFile(String uid);

    /**
     * Returns updated FileDescriptor
     */
    OperationResult<FileDescriptor> process(FileDescriptor file,
                                            SyncStrategy syncStrategy,
                                            OnConflictStrategy onConflictStrategy);
}
