package com.ivanovsky.passnotes.presentation.groups.sorting

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.domain.entity.SortDirection

class SortByTitleStrategy : SortStrategy {

    override fun sort(
        items: List<EncryptedDatabaseEntry>,
        direction: SortDirection,
        isGroupsAtStart: Boolean
    ): List<EncryptedDatabaseEntry> {
        return if (isGroupsAtStart) {
            val groups = items
                .filterGroups()
                .map { item -> Pair(item.title, item) }
                .sortedByWithDirection (direction) { it.first }
                .map { it.second }

            val notes = items
                .filterNotes()
                .map { item -> Pair(item.title, item) }
                .sortedByWithDirection(direction) { it.first }
                .map { it.second }

            groups + notes
        } else {
            items
                .map { item ->
                    when (item) {
                        is Group -> {
                            Pair(item.title, item)
                        }
                        is Note -> {
                            Pair(item.title, item)
                        }
                        else -> throw IllegalStateException()
                    }
                }
                .sortedByWithDirection (direction) { it.first }
                .map { it.second }
        }
    }
}