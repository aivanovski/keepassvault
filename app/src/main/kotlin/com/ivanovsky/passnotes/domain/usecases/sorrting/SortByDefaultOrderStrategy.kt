package com.ivanovsky.passnotes.domain.usecases.sorrting

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.domain.entity.SortDirection

class SortByDefaultOrderStrategy : SortStrategy {

    override fun sort(
        items: List<EncryptedDatabaseEntry>,
        direction: SortDirection,
        isGroupsAtStart: Boolean
    ): List<EncryptedDatabaseEntry> {
        return if (isGroupsAtStart) {
            val groups = items
                .filterGroups()
                .orderBy(direction)

            val notes = items
                .filterNotes()
                .orderBy(direction)

            groups + notes
        } else {
            items.orderBy(direction)
        }
    }
}