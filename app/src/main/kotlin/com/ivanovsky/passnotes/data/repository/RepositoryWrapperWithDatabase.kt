package com.ivanovsky.passnotes.data.repository

import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_GET_DATABASE
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import java.util.concurrent.atomic.AtomicReference

abstract class RepositoryWrapperWithDatabase : RepositoryWrapper {

    private val databaseRef = AtomicReference<EncryptedDatabase>()

    override fun onDatabaseOpened(db: EncryptedDatabase) {
        databaseRef.set(db)
    }

    override fun onDatabaseClosed() {
        databaseRef.set(null)
    }

    protected fun getDatabase(): EncryptedDatabase? = databaseRef.get()

    protected fun <T> noDatabaseError(): OperationResult<T> {
        return OperationResult.error(newDbError(MESSAGE_FAILED_TO_GET_DATABASE))
    }
}