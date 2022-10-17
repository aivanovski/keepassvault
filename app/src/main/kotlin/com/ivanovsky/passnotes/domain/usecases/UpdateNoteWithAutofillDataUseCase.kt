package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.NoteDiffer
import com.ivanovsky.passnotes.domain.PropertyFactory
import com.ivanovsky.passnotes.domain.PropertyFactory.createAutofillAppIdProperty
import com.ivanovsky.passnotes.extensions.addOrUpdateProperty
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import kotlinx.coroutines.withContext

class UpdateNoteWithAutofillDataUseCase(
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val noteDiffer: NoteDiffer,
    private val dispatchers: DispatcherProvider
) {

    suspend fun updateNoteWithAutofillData(
        note: Note,
        structure: AutofillStructure
    ): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            val newNote = addAutofillProperty(note, structure)

            val isModified = !noteDiffer.isEqualsByFields(
                note,
                newNote,
                NoteDiffer.ALL_FIELDS_WITHOUT_MODIFIED
            )

            if (isModified) {
                val updateResult = updateNoteUseCase.updateNote(newNote)
                if (updateResult.isFailed) {
                    return@withContext updateResult.takeError()
                }

                OperationResult.success(true)
            } else {
                OperationResult.success(false)
            }
        }

    private suspend fun addAutofillProperty(note: Note, structure: AutofillStructure): Note =
        withContext(dispatchers.IO) {
            var result = note

            if (structure.webDomain != null) {
                val newProperty = PropertyFactory.createUrlProperty(structure.webDomain)
                result = result.addOrUpdateProperty(newProperty)
            } else if (structure.applicationId != null) {
                val newProperty = createAutofillAppIdProperty(structure.applicationId)
                result = note.addOrUpdateProperty(newProperty)
            }

            result
        }
}