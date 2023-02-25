package com.ivanovsky.passnotes.domain.interactor.search

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.SearchType
import com.ivanovsky.passnotes.domain.search.EntryMatcher
import com.ivanovsky.passnotes.domain.search.Fzf4jFuzzyEntryMatcher
import com.ivanovsky.passnotes.domain.search.StrictEntryMatcher
import com.ivanovsky.passnotes.domain.usecases.CheckNoteAutofillDataUseCase
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.GetNoteUseCase
import com.ivanovsky.passnotes.domain.usecases.LockDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.SortGroupsAndNotesUseCase
import com.ivanovsky.passnotes.domain.usecases.UpdateNoteWithAutofillDataUseCase
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import java.util.UUID
import kotlinx.coroutines.withContext

class SearchInteractor(
    private val dispatchers: DispatcherProvider,
    private val getDbUseCase: GetDatabaseUseCase,
    private val getNoteUseCase: GetNoteUseCase,
    private val autofillUseCase: UpdateNoteWithAutofillDataUseCase,
    private val checkNoteAutofillDataUseCase: CheckNoteAutofillDataUseCase,
    private val lockUseCase: LockDatabaseUseCase,
    private val sortUseCase: SortGroupsAndNotesUseCase,
    private val settings: Settings
) {

    private val strictMatcher: EntryMatcher = StrictEntryMatcher()
    private val fuzzyMatcher: EntryMatcher = Fzf4jFuzzyEntryMatcher()

    suspend fun loadAllData(
        isRespectAutotypeProperty: Boolean
    ): OperationResult<List<EncryptedDatabaseEntry>> {
        return withContext(dispatchers.IO) {
            val dbResult = getDbUseCase.getDatabase()
            if (dbResult.isFailed) {
                return@withContext dbResult.mapError()
            }

            val db = dbResult.obj
            val getRootResult = db.groupDao.rootGroup
            if (getRootResult.isFailed) {
                return@withContext getRootResult.mapError()
            }

            val getAllNotesResult = db.noteDao.all
            if (getAllNotesResult.isFailed) {
                return@withContext getAllNotesResult.mapError()
            }

            val getAllGroupsResult = db.groupDao.all
            if (getAllGroupsResult.isFailed) {
                return@withContext getAllGroupsResult.mapError()
            }

            val root = getRootResult.obj
            val allNotes = getAllNotesResult.obj
            val allGroups = getAllGroupsResult.obj

            val searchableGroupMap = allGroups
                .filter { it.searchEnabled.isEnabled }
                .associateBy { it.uid }

            val searchableNotes = allNotes
                .filter { searchableGroupMap.containsKey(it.groupUid) }

            val items = if (isRespectAutotypeProperty) {
                val autotypeableGroups = searchableGroupMap.values
                    .filter { it.uid != root.uid && it.autotypeEnabled.isEnabled }

                val autotypeableGroupMap = searchableGroupMap.values
                    .filter { it.autotypeEnabled.isEnabled }
                    .associateBy { it.uid }

                val autotypeableNotes = searchableNotes
                    .filter { autotypeableGroupMap.containsKey(it.groupUid) }

                autotypeableNotes + autotypeableGroups
            } else {
                searchableNotes + searchableGroupMap.values.filter { it.uid != root.uid }
            }

            val result = sortUseCase.sortGroupsAndNotesAccordingToSettings(items)

            OperationResult.success(result)
        }
    }

    suspend fun filter(
        data: List<EncryptedDatabaseEntry>,
        query: String
    ): List<EncryptedDatabaseEntry> {
        if (query.isEmpty()) {
            return data
        }

        return withContext(dispatchers.IO) {
            when (settings.searchType) {
                SearchType.FUZZY -> {
                    fuzzyMatcher.match(
                        query = query,
                        entries = data
                    )
                }
                SearchType.STRICT -> {
                    strictMatcher.match(
                        query = query,
                        entries = data
                    )
                }
            }
        }
    }

    suspend fun getNoteByUid(noteUid: UUID): OperationResult<Note> =
        getNoteUseCase.getNoteByUid(noteUid)

    suspend fun updateNoteWithAutofillData(
        note: Note,
        structure: AutofillStructure
    ): OperationResult<Boolean> =
        autofillUseCase.updateNoteWithAutofillData(note, structure)

    suspend fun shouldUpdateNoteAutofillData(
        note: Note,
        structure: AutofillStructure
    ): Boolean =
        checkNoteAutofillDataUseCase.shouldUpdateNoteAutofillData(note, structure)

    fun lockDatabase() {
        lockUseCase.lockIfNeed()
    }
}