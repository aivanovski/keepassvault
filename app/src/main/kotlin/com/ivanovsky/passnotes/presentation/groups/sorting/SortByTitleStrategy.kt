package com.ivanovsky.passnotes.presentation.groups.sorting

import com.ivanovsky.passnotes.domain.entity.SortDirection
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor

class SortByTitleStrategy : SortStrategy {

    override fun sort(
        items: List<GroupsInteractor.Item>,
        direction: SortDirection,
        isGroupsAtStart: Boolean
    ): List<GroupsInteractor.Item> {
        return if (isGroupsAtStart) {
            val groups = items
                .filterGroups()
                .map { item -> Pair(item.group.title, item) }
                .sortedByWithDirection (direction) { it.first }
                .map { it.second }

            val notes = items
                .filterNotes()
                .map { item -> Pair(item.note.title, item) }
                .sortedByWithDirection(direction) { it.first }
                .map { it.second }

            groups + notes
        } else {
            items
                .map { item ->
                    when (item) {
                        is GroupsInteractor.GroupItem -> {
                            Pair(item.group.title, item)
                        }
                        is GroupsInteractor.NoteItem -> {
                            Pair(item.note.title, item)
                        }
                    }
                }
                .sortedByWithDirection (direction) { it.first }
                .map { it.second }
        }
    }
}