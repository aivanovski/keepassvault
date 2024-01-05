package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.util.UrlUtils
import kotlinx.coroutines.withContext
import timber.log.Timber

class FindNotesForAutofillUseCase(
    private val getDbUseCase: GetDatabaseUseCase,
    private val dispatchers: DispatcherProvider
) {

    suspend fun findNotesForAutofill(structure: AutofillStructure): OperationResult<List<Note>> =
        withContext(dispatchers.IO) {
            if (structure.webDomain.isNullOrEmpty() && structure.applicationId.isNullOrEmpty()) {
                return@withContext OperationResult.success(null)
            }

            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            val domain = structure.webDomain?.let { UrlUtils.extractCleanWebDomain(it) }
            val applicationId = structure.applicationId

            // TODO(autofill): to improve search, autofill-properties should be also checked after
            //  noteDao.find()

            Timber.d("domain=$domain, applicationId=$applicationId")

            val results = mutableListOf<Note>()
            if (applicationId != null) {
                val finByApplicationIdResult = db.noteDao.find(applicationId)
                if (finByApplicationIdResult.isFailed) {
                    return@withContext finByApplicationIdResult.takeError()
                }

                results.addAll(finByApplicationIdResult.obj)
            }

            if (!domain.isNullOrEmpty()) {
                val findByDomainResult = db.noteDao.find(domain)
                if (findByDomainResult.isFailed) {
                    return@withContext findByDomainResult.takeError()
                }

                results.addAll(findByDomainResult.obj)
            }

            OperationResult.success(results)
        }
}