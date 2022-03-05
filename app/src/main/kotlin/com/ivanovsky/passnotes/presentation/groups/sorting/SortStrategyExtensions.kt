package com.ivanovsky.passnotes.presentation.groups.sorting

import com.ivanovsky.passnotes.domain.entity.SortDirection
import com.ivanovsky.passnotes.domain.entity.SortDirection.ASCENDING
import com.ivanovsky.passnotes.domain.entity.SortDirection.DESCENDING
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor

fun <T, R : Comparable<R>> Iterable<T>.sortedByWithDirection(
    direction: SortDirection,
    selector: (T) -> R?
): List<T> {
    return when (direction) {
        ASCENDING -> this.sortedBy(selector)
        DESCENDING -> this.sortedByDescending(selector)
    }
}

fun <T> Iterable<T>.orderBy(direction: SortDirection): List<T> {
    return when (direction) {
        ASCENDING -> this.toList()
        DESCENDING -> this.reversed()
    }
}

fun List<GroupsInteractor.Item>.filterGroups(): List<GroupsInteractor.GroupItem> =
    filterIsInstance(GroupsInteractor.GroupItem::class.java)

fun List<GroupsInteractor.Item>.filterNotes(): List<GroupsInteractor.NoteItem> =
    filterIsInstance(GroupsInteractor.NoteItem::class.java)