package com.ivanovsky.passnotes.domain.usecases.diff.entity

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property

sealed interface DiffListItem {

    data class GroupItem(
        val parents: List<Group>,
        val event: DiffEvent<Group>
    ) : DiffListItem

    data class NoteItem(
        val parents: List<Group>,
        val event: DiffEvent<Note>
    ) : DiffListItem

    data class PropertiesItem(
        val parentGroups: List<Group>,
        val note: Note,
        val events: List<DiffEvent<Property>>
    ) : DiffListItem
}