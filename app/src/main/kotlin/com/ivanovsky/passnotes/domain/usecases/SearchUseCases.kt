package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.SearchType
import com.ivanovsky.passnotes.domain.search.EntryMatcher
import com.ivanovsky.passnotes.domain.search.Fzf4jFuzzyEntryMatcher
import com.ivanovsky.passnotes.domain.search.StrictEntryMatcher
import com.ivanovsky.passnotes.extensions.mapError
import kotlinx.coroutines.withContext

class SearchUseCases(
    private val dispatchers: DispatcherProvider,
    private val getDbUseCase: GetDatabaseUseCase,
    private val sortUseCase: SortGroupsAndNotesUseCase,
    private val settings: Settings
) {

    private val strictMatcher: EntryMatcher = StrictEntryMatcher()
    private val fuzzyMatcher: EntryMatcher = Fzf4jFuzzyEntryMatcher()

    suspend fun getAllSearchableEntries(
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

            val entries = if (isRespectAutotypeProperty) {
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

            val sortedEntries = sortUseCase.sortGroupsAndNotesAccordingToSettings(entries)

            OperationResult.success(sortedEntries)
        }
    }

    suspend fun filterEntries(
        entries: List<EncryptedDatabaseEntry>,
        query: String
    ): List<EncryptedDatabaseEntry> {
        if (query.isEmpty()) {
            return entries
        }

        return withContext(dispatchers.IO) {
            when (settings.searchType) {
                SearchType.FUZZY -> {
                    fuzzyMatcher.match(
                        query = query,
                        entries = entries
                    )
                }
                SearchType.STRICT -> {
                    strictMatcher.match(
                        query = query,
                        entries = entries
                    )
                }
            }
        }
    }
}