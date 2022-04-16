package com.ivanovsky.passnotes.presentation.groups.sorting

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.domain.entity.SortDirection
import com.ivanovsky.passnotes.domain.entity.SortDirection.ASCENDING
import com.ivanovsky.passnotes.domain.entity.SortDirection.DESCENDING

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

fun List<EncryptedDatabaseEntry>.filterGroups(): List<Group> =
    filterIsInstance(Group::class.java)

fun List<EncryptedDatabaseEntry>.filterNotes(): List<Note> =
    filterIsInstance(Note::class.java)