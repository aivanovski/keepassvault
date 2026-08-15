package com.ivanovsky.passnotes.data.repository.file.saf

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.MergeFiles
import com.ivanovsky.passnotes.data.entity.OperationError.newInvalidSyncProcessorUsage
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.RequestedSyncResolution
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.file.FileSystemSyncProcessor
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace

class SAFFileSystemSyncProcessor : FileSystemSyncProcessor {

    override fun getCachedFile(uid: String): FileDescriptor? {
        return null
    }

    override fun getSyncProgressStatusForFile(uid: String): SyncProgressStatus =
        SyncProgressStatus.IDLE

    override fun getSyncStatusForFile(uid: String): SyncStatus =
        SyncStatus.NO_CHANGES

    override fun getRevision(uid: String): String? = null

    override fun getMergeFiles(
        uid: String
    ): OperationResult<MergeFiles> =
        OperationResult.error(newInvalidSyncProcessorUsage(Stacktrace()))

    override fun getSyncConflictForFile(
        uid: String
    ): OperationResult<SyncConflictInfo> =
        OperationResult.error(newInvalidSyncProcessorUsage(Stacktrace()))

    override fun process(
        file: FileDescriptor,
        requestedResolution: RequestedSyncResolution
    ): OperationResult<FileDescriptor> =
        OperationResult.error(newInvalidSyncProcessorUsage(Stacktrace()))
}