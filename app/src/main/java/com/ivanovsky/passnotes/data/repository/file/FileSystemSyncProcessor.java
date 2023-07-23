package com.ivanovsky.passnotes.data.repository.file;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo;
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus;
import com.ivanovsky.passnotes.data.entity.SyncStatus;

public interface FileSystemSyncProcessor {

    @Nullable
    FileDescriptor getCachedFile(@NonNull String uid);

    @NonNull
    SyncProgressStatus getSyncProgressStatusForFile(@NonNull String uid);

    @NonNull
    SyncStatus getSyncStatusForFile(@NonNull String uid);

    @Nullable
    String getRevision(@NonNull String uid);

    @NonNull
    OperationResult<SyncConflictInfo> getSyncConflictForFile(@NonNull String uid);

    /** Returns updated FileDescriptor */
    @NonNull
    OperationResult<FileDescriptor> process(
            @NonNull FileDescriptor file,
            @NonNull SyncStrategy syncStrategy,
            @Nullable ConflictResolutionStrategy resolutionStrategy);
}
