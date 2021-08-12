package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.OperationError.newFailedToGetDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import kotlinx.coroutines.withContext

class GetDatabaseStatusUseCase(
    private val dbRepo: EncryptedDatabaseRepository,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getDatabaseStatus(): OperationResult<DatabaseStatus> {
        return withContext(dispatchers.IO) {
            val db = dbRepo.database
            if (db != null) {
                OperationResult.success(db.status)
            } else {
                OperationResult.error(newFailedToGetDbError())
            }
        }
    }
}