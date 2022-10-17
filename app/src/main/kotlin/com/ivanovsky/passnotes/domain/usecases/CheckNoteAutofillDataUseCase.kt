package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.util.UrlUtils.extractWebDomain
import kotlinx.coroutines.withContext

class CheckNoteAutofillDataUseCase(
    private val dispatchers: DispatcherProvider
) {

    suspend fun shouldUpdateNoteAutofillData(
        note: Note,
        structure: AutofillStructure
    ): Boolean =
        withContext(dispatchers.IO) {
            if (structure.webDomain != null) {
                val domain = extractWebDomain(structure.webDomain)
                val domains = URL_FILTER
                    .apply(note.properties)
                    .mapNotNull { property -> property.value?.let { extractWebDomain(it) } }

                !domains.contains(domain)
            } else if (structure.applicationId != null) {
                val appIds = APP_ID_FILTER
                    .apply(note.properties)
                    .mapNotNull { it.value }

                !appIds.contains(structure.applicationId)
            } else {
                true
            }
        }

    companion object {

        private val URL_FILTER = PropertyFilter.Builder()
            .filterByType(PropertyType.URL)
            .build()

        private val APP_ID_FILTER = PropertyFilter.Builder()
            .filterByName(Property.PROPERTY_NAME_AUTOFILL_APP_ID)
            .build()
    }
}