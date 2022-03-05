package com.ivanovsky.passnotes.presentation.groups.sorting

import com.ivanovsky.passnotes.domain.entity.SortDirection
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor

class SortByDefaultOrderStrategy : SortStrategy {

    override fun sort(
        items: List<GroupsInteractor.Item>,
        direction: SortDirection,
        isGroupsAtStart: Boolean
    ): List<GroupsInteractor.Item> {
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