package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.SyncStrategy
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext

class SyncUseCases(
    private val fileSystemResolver: FileSystemResolver,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getSyncConflictInfo(file: FileDescriptor): OperationResult<SyncConflictInfo> =
        withContext(dispatchers.IO) {
            fileSystemResolver
                .resolveSyncProcessor(file.fsAuthority)
                .getSyncConflictForFile(file.uid)
        }

    suspend fun getSyncState(file: FileDescriptor): SyncState =
        withContext(dispatchers.IO) {
            val syncProcessor = fileSystemResolver.resolveSyncProcessor(file.fsAuthority)

            val status = syncProcessor.getSyncStatusForFile(file.uid)
            val progress = syncProcessor.getSyncProgressStatusForFile(file.uid)

            SyncState(status, progress)
        }

    suspend fun resolveConflict(
        file: FileDescriptor,
        resolutionStrategy: ConflictResolutionStrategy
    ): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            fileSystemResolver
                .resolveSyncProcessor(file.fsAuthority)
                .process(file, SyncStrategy.LAST_REMOTE_MODIFICATION_WINS, resolutionStrategy)
        }

    suspend fun processSync(file: FileDescriptor): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            fileSystemResolver
                .resolveSyncProcessor(file.fsAuthority)
                .process(file, SyncStrategy.LAST_REMOTE_MODIFICATION_WINS, null)
        }
}