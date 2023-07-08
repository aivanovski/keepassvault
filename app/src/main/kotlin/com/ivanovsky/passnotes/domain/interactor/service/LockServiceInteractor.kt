package com.ivanovsky.passnotes.domain.interactor.service

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.GetLastSyncStatusUseCase
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases
import kotlinx.coroutines.withContext

class LockServiceInteractor(
    private val getLastSyncStatusUseCase: GetLastSyncStatusUseCase,
    private val dbRepository: EncryptedDatabaseRepository,
    private val lockInteractor: DatabaseLockInteractor,
    private val syncUseCases: SyncUseCases,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getLastSyncStatus(): OperationResult<SyncStatus?> =
        getLastSyncStatusUseCase.getLastSyncStatus()

    suspend fun syncAndLock(file: FileDescriptor): OperationResult<Unit> =
        withContext(dispatchers.IO) {
            val syncResult = syncUseCases.processSync(file)
            if (syncResult.isFailed) {
                return@withContext syncResult.takeError()
            }

            val close = dbRepository.close()
            if (close.isFailed) {
                return@withContext close.takeError()
            }

            lockInteractor.stopServiceIfNeed()

            OperationResult.success(Unit)
        }
}