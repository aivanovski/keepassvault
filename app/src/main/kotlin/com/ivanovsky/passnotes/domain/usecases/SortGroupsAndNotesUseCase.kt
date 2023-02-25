package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.SortDirection
import com.ivanovsky.passnotes.domain.entity.SortType
import com.ivanovsky.passnotes.domain.usecases.sorrting.SortByDateStrategy
import com.ivanovsky.passnotes.domain.usecases.sorrting.SortByDefaultOrderStrategy
import com.ivanovsky.passnotes.domain.usecases.sorrting.SortByTitleStrategy
import kotlinx.coroutines.withContext

class SortGroupsAndNotesUseCase(
    private val settings: Settings,
    private val dispatchers: DispatcherProvider
) {

    suspend fun sortGroupsAndNotesAccordingToSettings(
        items: List<EncryptedDatabaseEntry>
    ): List<EncryptedDatabaseEntry> =
        sortGroupsAndNotes(
            items = items,
            sortType = settings.sortType,
            direction = settings.sortDirection,
            isGroupsAtStart = settings.isGroupsAtStartEnabled
        )

    suspend fun sortGroupsAndNotes(
        items: List<EncryptedDatabaseEntry>,
        sortType: SortType,
        direction: SortDirection,
        isGroupsAtStart: Boolean
    ): List<EncryptedDatabaseEntry> =
        withContext(dispatchers.IO) {
            when (sortType) {
                SortType.DEFAULT -> SortByDefaultOrderStrategy().sort(
                    items,
                    direction,
                    isGroupsAtStart = isGroupsAtStart
                )
                SortType.TITLE -> SortByTitleStrategy().sort(
                    items,
                    direction,
                    isGroupsAtStart = isGroupsAtStart
                )
                SortType.CREATION_DATE -> SortByDateStrategy(SortByDateStrategy.Type.CREATION_DATE)
                    .sort(
                        items,
                        direction,
                        isGroupsAtStart = isGroupsAtStart
                    )
                SortType.MODIFICATION_DATE -> SortByDateStrategy(
                    SortByDateStrategy.Type.MODIFICATION_DATE
                ).sort(
                    items,
                    direction,
                    isGroupsAtStart = isGroupsAtStart
                )
            }
        }
}