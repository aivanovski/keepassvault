package com.ivanovsky.passnotes.domain.interactor.service

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.keepass.DatabaseSyncStateProvider
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases
import com.ivanovsky.passnotes.extensions.getOrThrow
import kotlinx.coroutines.withContext
import timber.log.Timber

class LockServiceInteractor(
    private val syncStateProvider: DatabaseSyncStateProvider,
    private val dbRepository: EncryptedDatabaseRepository,
    private val lockInteractor: DatabaseLockInteractor,
    private val syncUseCases: SyncUseCases,
    private val dispatchers: DispatcherProvider
) {

    fun getLastSyncState(): SyncState? =
        syncStateProvider.syncState

    suspend fun syncAndLockIfNeed(file: FileDescriptor): OperationResult<Unit> =
        withContext(dispatchers.IO) {
            val isSyncNeededResult = syncUseCases.isSyncNeeded(file)
            if (isSyncNeededResult.isFailed) {
                return@withContext isSyncNeededResult.takeError()
            }

            val isSyncNeeded = isSyncNeededResult.getOrThrow()
            Timber.d("isSyncNeeded=%s", isSyncNeeded)

            if (isSyncNeeded) {
                val syncResult = syncUseCases.processSync(file)
                if (syncResult.isFailed) {
                    return@withContext syncResult.takeError()
                }
            }

            val close = dbRepository.close()
            if (close.isFailed) {
                return@withContext close.takeError()
            }

            lockInteractor.stopServiceIfNeed()

            OperationResult.success(Unit)
        }
}