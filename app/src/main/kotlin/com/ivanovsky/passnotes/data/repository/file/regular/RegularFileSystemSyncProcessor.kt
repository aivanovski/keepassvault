package com.ivanovsky.passnotes.data.repository.file.regular

import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_INCORRECT_USE_CASE
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemSyncProcessor
import com.ivanovsky.passnotes.data.repository.file.SyncStrategy
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace

class RegularFileSystemSyncProcessor(
    private val provider: RegularFileSystemProvider
) : FileSystemSyncProcessor {

    override fun getCachedFile(uid: String): FileDescriptor? {
        return null
    }

    override fun getSyncProgressStatusForFile(uid: String): SyncProgressStatus =
        SyncProgressStatus.IDLE

    override fun getRevision(uid: String): String? = null

    override fun getSyncStatusForFile(uid: String): SyncStatus {
        val getFileResult = provider.getFile(uid, FSOptions.noCache())

        return when {
            getFileResult.isSucceeded -> SyncStatus.NO_CHANGES
            getFileResult.isFailed &&
                getFileResult.error.type == OperationError.Type.FILE_NOT_FOUND_ERROR -> {
                SyncStatus.FILE_NOT_FOUND
            }

            else -> SyncStatus.ERROR
        }
    }

    override fun getSyncConflictForFile(uid: String): OperationResult<SyncConflictInfo> {
        return OperationResult.error(
            newGenericError(
                MESSAGE_INCORRECT_USE_CASE,
                Stacktrace()
            )
        )
    }

    override fun process(
        file: FileDescriptor,
        syncStrategy: SyncStrategy,
        resolutionStrategy: ConflictResolutionStrategy?
    ): OperationResult<FileDescriptor> {
        return OperationResult.error(
            newGenericError(
                MESSAGE_INCORRECT_USE_CASE,
                Stacktrace()
            )
        )
    }
}