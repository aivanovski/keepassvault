package com.ivanovsky.passnotes.presentation.groups.sorting

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.domain.entity.SortDirection

interface SortStrategy {
    fun sort(
        items: List<EncryptedDatabaseEntry>,
        direction: SortDirection,
        isGroupsAtStart: Boolean
    ): List<EncryptedDatabaseEntry>
}