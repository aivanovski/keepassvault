package com.ivanovsky.passnotes.data.repository.file.saf

import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_INCORRECT_USE_CASE
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.file.FileSystemSyncProcessor
import com.ivanovsky.passnotes.data.repository.file.SyncStrategy

class SAFFileSystemSyncProcessor : FileSystemSyncProcessor {

    override fun getCachedFile(uid: String): FileDescriptor? {
        return null
    }

    override fun getSyncProgressStatusForFile(uid: String): SyncProgressStatus =
        SyncProgressStatus.IDLE

    override fun getSyncStatusForFile(uid: String): SyncStatus =
        SyncStatus.NO_CHANGES

    override fun getRevision(uid: String): String? = null

    override fun getSyncConflictForFile(uid: String): OperationResult<SyncConflictInfo> {
        return OperationResult.error(newGenericError(MESSAGE_INCORRECT_USE_CASE))
    }

    override fun process(
        file: FileDescriptor,
        syncStrategy: SyncStrategy,
        resolutionStrategy: ConflictResolutionStrategy?
    ): OperationResult<FileDescriptor> {
        return OperationResult.error(newGenericError(MESSAGE_INCORRECT_USE_CASE))
    }
}