package com.ivanovsky.passnotes.domain.usecases

import arrow.core.raise.either
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseConfig
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.SearchOptions
import com.ivanovsky.passnotes.domain.entity.SearchScope
import com.ivanovsky.passnotes.domain.entity.SearchType
import com.ivanovsky.passnotes.domain.search.Fzf4jFuzzyEntryMatcher
import com.ivanovsky.passnotes.domain.search.StrictEntryMatcher
import com.ivanovsky.passnotes.extensions.toEither
import com.ivanovsky.passnotes.util.toOperationResult
import java.util.LinkedList
import java.util.UUID
import kotlinx.coroutines.withContext

class SearchUseCases(
    private val dispatchers: DispatcherProvider,
    private val getDbUseCase: GetDatabaseUseCase,
    private val sortUseCase: SortGroupsAndNotesUseCase
) {

    private val strictMatcher = StrictEntryMatcher()
    private val fuzzyMatcher = Fzf4jFuzzyEntryMatcher()

    suspend fun getAllSearchableEntries(
        options: SearchOptions,
        isRespectAutotypeProperty: Boolean
    ): OperationResult<List<EncryptedDatabaseEntry>> =
        withContext(dispatchers.IO) {
            either {
                val db = getDbUseCase.getDatabase().toEither().bind()
                val root = db.groupDao.rootGroup.toEither().bind()
                val allNotes = db.noteDao.all.toEither().bind()
                val allGroups = db.groupDao.all.toEither().bind()
                val dbConfig = db.config.toEither().bind()

                val recycledGroupUids = filterRecycledGroups(
                    config = dbConfig,
                    allGroups = allGroups
                )

                val isOnlySearchable = options.restrictionScopes.contains(SearchScope.SEARCHABLE)
                val isIncludeRecycled = options.restrictionScopes.contains(SearchScope.RECYCLE_BIN)

                val searchableGroupMap = allGroups
                    .filter { group ->
                        val isGroupRecycled = recycledGroupUids.contains(group.uid)

                        when {
                            !isIncludeRecycled && isGroupRecycled -> false
                            isOnlySearchable -> group.searchEnabled.isEnabled
                            else -> true
                        }
                    }
                    .associateBy { group -> group.uid }

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
                    searchableNotes +
                        searchableGroupMap.values.filter { group -> group.uid != root.uid }
                }

                val sortedEntries = sortUseCase.sortGroupsAndNotesAccordingToSettings(entries)

                sortedEntries
            }.toOperationResult()
        }

    private fun filterRecycledGroups(
        config: EncryptedDatabaseConfig,
        allGroups: List<Group>
    ): Set<UUID> {
        val groupUidToChildGroupsMap = createGroupUidToChildGroupsMap(allGroups)

        val recycleBinUid = config.recycleBinUid
        val recycledGroupUids = if (recycleBinUid != null) {
            filterGroupTree(
                targetGroup = recycleBinUid,
                groupUidToChildGroupsMap = groupUidToChildGroupsMap
            )
                .toMutableSet()
                .apply {
                    add(recycleBinUid)
                }
        } else {
            emptySet()
        }

        return recycledGroupUids
    }

    private fun createGroupUidToChildGroupsMap(groups: List<Group>): Map<UUID, List<UUID>> {
        val result = HashMap<UUID, MutableList<UUID>>()

        for (group in groups) {
            val parentUid = group.parentUid ?: continue

            result[parentUid] = result.getOrDefault(parentUid, mutableListOf())
                .apply {
                    add(group.uid)
                }
        }

        return result
    }

    private fun filterGroupTree(
        targetGroup: UUID,
        groupUidToChildGroupsMap: Map<UUID, List<UUID>>
    ): List<UUID> {
        val result = mutableListOf<UUID>()

        val queue = LinkedList<UUID>()
            .apply {
                add(targetGroup)
            }

        while (queue.isNotEmpty()) {
            repeat(queue.size) {
                val uid = queue.removeFirst()
                val childUids = groupUidToChildGroupsMap[uid] ?: emptyList()

                result.addAll(childUids)

                for (childUid in childUids) {
                    queue.add(childUid)
                }
            }
        }

        return result
    }

    suspend fun filterEntries(
        options: SearchOptions,
        entries: List<EncryptedDatabaseEntry>,
        query: String
    ): List<EncryptedDatabaseEntry> {
        if (query.isEmpty()) {
            return entries
        }

        return withContext(dispatchers.IO) {
            when (options.searchType) {
                SearchType.FUZZY -> fuzzyMatcher.match(options, query, entries)
                SearchType.STRICT -> strictMatcher.match(options, query, entries)
            }
        }
    }
}