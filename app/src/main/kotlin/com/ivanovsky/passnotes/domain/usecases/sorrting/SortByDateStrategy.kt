package com.ivanovsky.passnotes.domain.usecases.sorrting

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.domain.entity.SortDirection

class SortByDateStrategy(
    private val type: Type
) : SortStrategy {

    override fun sort(
        items: List<EncryptedDatabaseEntry>,
        direction: SortDirection,
        isGroupsAtStart: Boolean
    ): List<EncryptedDatabaseEntry> {
        val groups = items.filterIsInstance(Group::class.java)

        val notes = items.filterIsInstance(Note::class.java)
            .map { item ->
                val date = when (type) {
                    Type.CREATION_DATE -> item.created
                    Type.MODIFICATION_DATE -> item.modified
                }
                Pair(date, item)
            }
            .sortedByWithDirection(direction) { it.first }
            .map { it.second }

        return groups + notes
    }

    enum class Type {
        CREATION_DATE,
        MODIFICATION_DATE
    }
}