package com.ivanovsky.passnotes.domain.usecases.sort_groups_and_notes

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.domain.entity.SortDirection

interface SortStrategy {
    fun sort(
        items: List<EncryptedDatabaseEntry>,
        direction: SortDirection,
        isGroupsAtStart: Boolean
    ): List<EncryptedDatabaseEntry>
}