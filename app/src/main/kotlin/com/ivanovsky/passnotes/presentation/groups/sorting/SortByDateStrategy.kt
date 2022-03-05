package com.ivanovsky.passnotes.presentation.groups.sorting

import com.ivanovsky.passnotes.domain.entity.SortDirection
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor

class SortByDateStrategy(
    private val type: Type
) : SortStrategy {

    override fun sort(
        items: List<GroupsInteractor.Item>,
        direction: SortDirection,
        isGroupsAtStart: Boolean
    ): List<GroupsInteractor.Item> {
        val groups = items.filterIsInstance(GroupsInteractor.GroupItem::class.java)

        val notes = items.filterIsInstance(GroupsInteractor.NoteItem::class.java)
            .map { item ->
                val date = when (type) {
                    Type.CREATION_DATE -> item.note.created
                    Type.MODIFICATION_DATE -> item.note.modified
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