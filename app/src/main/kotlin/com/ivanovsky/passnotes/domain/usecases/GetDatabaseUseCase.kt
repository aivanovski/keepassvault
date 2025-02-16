package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_GET_DATABASE
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import kotlinx.coroutines.withContext

class GetDatabaseUseCase(
    private val dbRepo: EncryptedDatabaseRepository,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getDatabase(): OperationResult<EncryptedDatabase> {
        return withContext(dispatchers.IO) {
            getDatabaseSynchronously()
        }
    }

    fun getDatabaseSynchronously(): OperationResult<EncryptedDatabase> {
        val db = dbRepo.database
        return if (db != null) {
            OperationResult.success(db)
        } else {
            OperationResult.error(
                newDbError(MESSAGE_FAILED_TO_GET_DATABASE, Stacktrace())
            )
        }
    }
}