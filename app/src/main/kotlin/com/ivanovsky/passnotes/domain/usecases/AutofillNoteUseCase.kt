package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import kotlinx.coroutines.withContext

class AutofillNoteUseCase(
    private val dispatcher: DispatcherProvider
) {

    suspend fun isNoteBoundWithStructure(note: Note, structure: AutofillStructure): Boolean =
        withContext(dispatcher.IO) {
            if (structure.applicationId.isNullOrEmpty() && structure.webDomain.isNullOrEmpty()) {
                return@withContext false
            }

            if (structure.applicationId != null) {
                val filter = PropertyFilter.Builder()
                    .filterAutofillAppId()
                    .build()

                val appIdProperty = filter
                    .apply(note.properties)
                    .firstOrNull()

                if (appIdProperty?.value == structure.applicationId) {
                    return@withContext true
                }
            }

            if (structure.webDomain != null) {
                val filter = PropertyFilter.Builder()
                    .filterUrl()
                    .build()

                val urlProperty = filter
                    .apply(note.properties)
                    .firstOrNull()

                if (urlProperty?.value == structure.webDomain) {
                    return@withContext true
                }
            }

            false
        }
}