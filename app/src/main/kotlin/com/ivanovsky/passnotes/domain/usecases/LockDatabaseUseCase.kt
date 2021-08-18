package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector.inject

class LockDatabaseUseCase {

    private val dbRepository: EncryptedDatabaseRepository by inject()
    private val lockInteractor: DatabaseLockInteractor by inject()

    fun lockIfNeed(): OperationResult<Unit> {
        if (!dbRepository.isOpened) {
            return OperationResult.success(Unit)
        }

        val close = dbRepository.close()
        if (close.isFailed) {
            return close.takeError()
        }

        lockInteractor.stopServiceIfNeed()

        return close.takeStatusWith(Unit)
    }
}