package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.SyncStrategy
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.extensions.isSameFile
import kotlinx.coroutines.withContext

class SyncUseCases(
    private val fileSystemResolver: FileSystemResolver,
    private val dispatchers: DispatcherProvider,
    private val dbRepo: EncryptedDatabaseRepository
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
            val revision = syncProcessor.getRevision(file.uid)

            SyncState(status, progress, revision)
        }

    suspend fun getSyncProgressStatus(file: FileDescriptor): SyncProgressStatus =
        withContext(dispatchers.IO) {
            val syncProcessor = fileSystemResolver.resolveSyncProcessor(file.fsAuthority)
            syncProcessor.getSyncProgressStatusForFile(file.uid)
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

    suspend fun isSyncNeeded(file: FileDescriptor): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            val syncState = getSyncState(file)

            val isNeeded = (syncState.status == SyncStatus.LOCAL_CHANGES ||
                syncState.status == SyncStatus.REMOTE_CHANGES)

            OperationResult.success(isNeeded)
        }

    suspend fun processSync(file: FileDescriptor): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            val syncProcessor = fileSystemResolver.resolveSyncProcessor(file.fsAuthority)
            val processResult = syncProcessor.process(
                file,
                SyncStrategy.LAST_REMOTE_MODIFICATION_WINS,
                null
            )

            // Reload database if it was changed
            val db = dbRepo.database
            if (processResult.isSucceeded &&
                db != null &&
                db.file.isSameFile(file)
            ) {
                dbRepo.reload()
            }

            processResult
        }
}