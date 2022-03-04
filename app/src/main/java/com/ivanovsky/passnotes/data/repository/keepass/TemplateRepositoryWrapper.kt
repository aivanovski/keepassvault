package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_GET_DATABASE
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.repository.RepositoryWrapper
import com.ivanovsky.passnotes.data.repository.TemplateRepository
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class TemplateRepositoryWrapper : TemplateRepository, RepositoryWrapper {

    private val databaseRef = AtomicReference<EncryptedDatabase>()

    override fun onDatabaseOpened(db: EncryptedDatabase) {
        databaseRef.set(db)
    }

    override fun onDatabaseClosed() {
        databaseRef.set(null)
    }

    override fun getTemplateGroupUid(): OperationResult<UUID?> {
        val db = databaseRef.get() ?: return databaseIsClosedError()

        return db.templateRepository.getTemplateGroupUid()
    }

    override fun getTemplates(): OperationResult<List<Template>> {
        val db = databaseRef.get() ?: return databaseIsClosedError()

        return db.templateRepository.getTemplates()
    }

    override fun addTemplates(templates: List<Template>): OperationResult<Boolean> {
        val db = databaseRef.get() ?: return databaseIsClosedError()

        return db.templateRepository.addTemplates(templates)
    }

    private fun <T> databaseIsClosedError(): OperationResult<T> {
        return OperationResult.error(newDbError(MESSAGE_FAILED_TO_GET_DATABASE))
    }
}