package com.ivanovsky.passnotes.domain.interactor.service

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseStatusUseCase
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases
import kotlinx.coroutines.withContext
import timber.log.Timber

class LockServiceInteractor(
    private val getDbUseCase: GetDatabaseUseCase,
    private val getStatusUseCase: GetDatabaseStatusUseCase,
    private val dbRepository: EncryptedDatabaseRepository,
    private val lockInteractor: DatabaseLockInteractor,
    private val syncUseCases: SyncUseCases,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getDatabaseStatus(): OperationResult<DatabaseStatus> =
        getStatusUseCase.getDatabaseStatus()

    suspend fun syncAndLock(file: FileDescriptor): OperationResult<Unit> =
        withContext(dispatchers.IO) {
            Timber.d("file=$file")

            val syncResult = syncUseCases.processSync(file)
            Timber.d("syncResult=$syncResult")

            if (syncResult.isFailed) {
                return@withContext syncResult.takeError()
            }


            val close = dbRepository.close()
            Timber.d("close=$close")
            if (close.isFailed) {
                return@withContext close.takeError()
            }

            lockInteractor.stopServiceIfNeed()

            OperationResult.success(Unit)
        }
}