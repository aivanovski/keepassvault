package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.util.UrlUtils
import kotlinx.coroutines.withContext

class FindNoteForAutofillUseCase(
    private val getDbUseCase: GetDatabaseUseCase,
    private val dispatchers: DispatcherProvider
) {

    suspend fun findNoteForAutofill(structure: AutofillStructure): OperationResult<Note?> =
        withContext(dispatchers.IO) {
            if (structure.webDomain.isNullOrEmpty() && structure.applicationId.isNullOrEmpty()) {
                return@withContext OperationResult.success(null)
            }

            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            val domain = structure.webDomain?.let { UrlUtils.extractWebDomain(it) }
            val applicationId = structure.applicationId

            // TODO(autofill): to improve search, autofill-properties should be also checked after
            //  noteDao.find()
            if (applicationId != null) {
                val findResult = db.noteDao.find(applicationId)
                if (findResult.isFailed) {
                    return@withContext findResult.takeError()
                }

                val notes = findResult.obj
                if (notes.isNotEmpty()) {
                    return@withContext OperationResult.success(notes.firstOrNull())
                }
            }

            if (domain != null && domain.isNotEmpty()) {
                val findResult = db.noteDao.find(domain)
                if (findResult.isFailed) {
                    return@withContext findResult.takeError()
                }

                val notes = findResult.obj
                if (notes.isNotEmpty()) {
                    return@withContext OperationResult.success(notes.firstOrNull())
                }
            }

            OperationResult.success(null)
        }
}