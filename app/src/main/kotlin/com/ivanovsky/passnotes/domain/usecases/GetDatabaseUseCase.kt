package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_GET_DATABASE
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace

class GetDatabaseUseCase(
    private val dbRepo: EncryptedDatabaseRepository
) {

    fun getDatabase(): OperationResult<EncryptedDatabase> =
        getDatabaseSynchronously()

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