package com.ivanovsky.passnotes.domain.usecases

import arrow.core.Either
import arrow.core.raise.either
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.file.SyncStrategy
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.extensions.getFileDescriptor
import com.ivanovsky.passnotes.extensions.isSameFile
import com.ivanovsky.passnotes.extensions.isSyncable
import com.ivanovsky.passnotes.extensions.toEither
import kotlinx.coroutines.withContext
import timber.log.Timber

class SyncUseCases(
    private val fileSystemResolver: FileSystemResolver,
    private val dispatchers: DispatcherProvider,
    private val dbRepo: EncryptedDatabaseRepository,
    private val usedFileRepository: UsedFileRepository
) {

    suspend fun syncChanges(): Either<OperationError, Unit> =
        withContext(dispatchers.IO) {
            either {
                val syncableFiles = usedFileRepository.getAll()
                    .map { file -> file.getFileDescriptor() }
                    .filter { file -> file.fsAuthority.isSyncable() }

                for (file in syncableFiles) {
                    val provider = fileSystemResolver.resolveProvider(file.fsAuthority)
                    val cachedFile = provider.syncProcessor.getCachedFile(file.uid)

                    if (cachedFile == null) {
                        // The file isn't downloaded, it should be downloaded first
                        Timber.d(
                            "Syncing file: file=%s, fsType=%s".format(
                                file.path,
                                file.fsAuthority.type
                            )
                        )

                        val content = provider.openFileForRead(
                            file,
                            OnConflictStrategy.CANCEL,
                            FSOptions.READ_ONLY
                        ).toEither().bind()

                        Either.catch { content.close() }
                            .mapLeft { error -> newGenericError(error) }
                            .bind()
                    } else {
                        val syncState = getSyncState(file)

                        val hasRemoteChanges = (syncState.status == SyncStatus.REMOTE_CHANGES)
                        val hasLocalChanges = (syncState.status == SyncStatus.LOCAL_CHANGES)

                        Timber.d(
                            "Syncing file: syncState=%s, file=%s, fsType=%s".format(
                                syncState,
                                file.path,
                                file.fsAuthority.type
                            )
                        )

                        if (hasRemoteChanges || hasLocalChanges) {
                            processSync(file).toEither().bind()
                        }
                    }
                }
            }
        }

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

            val isNeeded = (
                syncState.status == SyncStatus.LOCAL_CHANGES ||
                    syncState.status == SyncStatus.REMOTE_CHANGES
                )

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