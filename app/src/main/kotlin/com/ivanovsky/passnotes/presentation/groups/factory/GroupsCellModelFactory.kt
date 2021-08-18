package com.ivanovsky.passnotes.presentation.groups.factory

import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.presentation.core.factory.CellModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.GroupCellModel
import com.ivanovsky.passnotes.presentation.core.model.NoteCellModel

class GroupsCellModelFactory : CellModelFactory<List<GroupsInteractor.Item>> {

    override fun createCellModels(data: List<GroupsInteractor.Item>): List<BaseCellModel> {
        return data.mapNotNull { item ->
            when (item) {
                is GroupsInteractor.GroupItem -> {
                    GroupCellModel(
                        id = item.group.uid.toString(),
                        group = item.group,
                        noteCount = item.noteCount,
                        childGroupCount = item.childGroupCount
                    )
                }
                is GroupsInteractor.NoteItem -> {
                    NoteCellModel(
                        id = item.note.uid?.toString() ?: "",
                        note = item.note
                    )
                }
                else -> null
            }
        }
    }
}
