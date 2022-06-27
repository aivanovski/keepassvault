package com.ivanovsky.passnotes.domain.interactor.search

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.UpdateNoteWithAutofillDataUseCase
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.GetNoteUseCase
import com.ivanovsky.passnotes.domain.usecases.LockDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.SortGroupsAndNotesUseCase
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import kotlinx.coroutines.withContext
import java.util.UUID

class SearchInteractor(
    private val dispatchers: DispatcherProvider,
    private val getDbUseCase: GetDatabaseUseCase,
    private val getNoteUseCase: GetNoteUseCase,
    private val autofillUseCase: UpdateNoteWithAutofillDataUseCase,
    private val lockUseCase: LockDatabaseUseCase,
    private val sortUseCase: SortGroupsAndNotesUseCase
) {

    suspend fun find(query: String): OperationResult<List<EncryptedDatabaseEntry>> {
        return withContext(dispatchers.IO) {
            val dbResult = getDbUseCase.getDatabase()
            if (dbResult.isFailed) {
                return@withContext dbResult.takeError()
            }

            val db = dbResult.obj
            val notesResult = db.noteRepository.find(query)
            if (notesResult.isFailed) {
                return@withContext notesResult.takeError()
            }

            val groupsResult = db.groupDao.find(query)
            if (groupsResult.isFailed) {
                return@withContext groupsResult.takeError()
            }

            val groups = groupsResult.obj
            val notes = notesResult.obj

            OperationResult.success(groups + notes)
        }
    }

    suspend fun sort(
        items: List<EncryptedDatabaseEntry>
    ): List<EncryptedDatabaseEntry> =
        sortUseCase.sortGroupsAndNotesAccordingToSettings(items)

    suspend fun getNoteByUid(noteUid: UUID): OperationResult<Note> =
        getNoteUseCase.getNoteByUid(noteUid)

    suspend fun updateNoteWithAutofillData(
        note: Note,
        structure: AutofillStructure
    ): OperationResult<Boolean> =
        autofillUseCase.updateNoteWithAutofillData(note, structure)

    fun lockDatabase() {
        lockUseCase.lockIfNeed()
    }
}