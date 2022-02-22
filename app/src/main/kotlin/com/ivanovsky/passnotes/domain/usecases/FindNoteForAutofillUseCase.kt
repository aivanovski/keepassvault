package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.util.StringUtils.DOT
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.withContext

class FindNoteForAutofillUseCase(
    private val getDbUseCase: GetDatabaseUseCase,
    private val dispatchers: DispatcherProvider
) {

    suspend fun findNoteForAutofill(structure: AutofillStructure): OperationResult<Pair<Boolean, Note?>> =
        withContext(dispatchers.IO) {
            if (structure.webDomain.isNullOrEmpty()) {
                return@withContext OperationResult.success(Pair(false, null))
            }

            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            val domain = getCleanWebDomain(structure.webDomain)

            val findNotesResult = db.noteRepository.find(domain)
            if (findNotesResult.isFailed) {
                return@withContext findNotesResult.takeError()
            }

            val note = findNotesResult.obj.firstOrNull()

            OperationResult.success(Pair(note != null, note))
        }

    private fun getCleanWebDomain(webDomain: String): String {
        val lastDotIdx = webDomain.lastIndexOf(DOT)
        if (lastDotIdx == -1) {
            return EMPTY
        }

        return when {
            lastDotIdx > 0 && lastDotIdx <= webDomain.length - 1 -> {
                webDomain.substring(0, lastDotIdx)
            }
            else -> webDomain
        }
    }
}