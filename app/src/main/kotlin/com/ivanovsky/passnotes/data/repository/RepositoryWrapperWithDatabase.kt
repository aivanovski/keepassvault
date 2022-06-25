package com.ivanovsky.passnotes.data.repository

import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_GET_DATABASE
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase

abstract class RepositoryWrapperWithDatabase(
    private val dbRepo: EncryptedDatabaseRepository
) {

    protected fun getDatabase(): EncryptedDatabase? = dbRepo.database

    protected fun <T> noDatabaseError(): OperationResult<T> {
        return OperationResult.error(newDbError(MESSAGE_FAILED_TO_GET_DATABASE))
    }
}