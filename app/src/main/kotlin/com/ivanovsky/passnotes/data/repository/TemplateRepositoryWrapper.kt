package com.ivanovsky.passnotes.data.repository

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Template
import java.util.UUID

class TemplateRepositoryWrapper : RepositoryWrapperWithDatabase(), TemplateRepository {

    override fun getTemplateGroupUid(): OperationResult<UUID?> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.templateRepository.getTemplateGroupUid()
    }

    override fun getTemplates(): OperationResult<List<Template>> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.templateRepository.getTemplates()
    }

    override fun addTemplates(templates: List<Template>): OperationResult<Boolean> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.templateRepository.addTemplates(templates)
    }

    override fun addTemplates(
        templates: List<Template>,
        doCommit: Boolean
    ): OperationResult<Boolean> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.templateRepository.addTemplates(templates, doCommit)
    }
}