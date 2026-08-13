package com.ivanovsky.passnotes.data.repository.file

import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.MergeFiles
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncStatus

interface FileSystemSyncProcessor {

    fun getCachedFile(uid: String): FileDescriptor?

    fun getSyncProgressStatusForFile(uid: String): SyncProgressStatus

    fun getSyncStatusForFile(uid: String): SyncStatus

    fun getRevision(uid: String): String?

    fun getSyncConflictForFile(uid: String): OperationResult<SyncConflictInfo>

    fun getMergeFiles(uid: String): OperationResult<MergeFiles>

    /** Returns the updated [FileDescriptor]. */
    fun process(
        file: FileDescriptor,
        syncStrategy: SyncStrategy,
        resolutionStrategy: ConflictResolutionStrategy?
    ): OperationResult<FileDescriptor>
}