package com.ivanovsky.passnotes.data.repository.file;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo;
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy;
import com.ivanovsky.passnotes.data.entity.SyncStatus;

import java.util.List;

public interface FileSystemSyncProcessor {

    @NonNull
    List<FileDescriptor> getLocallyModifiedFiles();

    @NonNull
    SyncStatus getSyncStatusForFile(@NonNull String uid);

    @NonNull
    OperationResult<SyncConflictInfo> getSyncConflictForFile(@NonNull String uid);

    /**
     * Returns updated FileDescriptor
     */
    @NonNull
    OperationResult<FileDescriptor> process(@NonNull FileDescriptor file,
                                            @NonNull SyncStrategy syncStrategy,
                                            @Nullable ConflictResolutionStrategy resolutionStrategy);
}
