package com.ivanovsky.passnotes.presentation.groups.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.presentation.core.factory.CellModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.GroupCellModel
import com.ivanovsky.passnotes.presentation.core.model.NoteCellModel

class GroupsCellModelFactory(
    private val resourceProvider: ResourceProvider
) : CellModelFactory<List<GroupsInteractor.Item>> {

    override fun createCellModels(data: List<GroupsInteractor.Item>): List<BaseCellModel> {
        return data.mapNotNull { item ->
            when (item) {
                is GroupsInteractor.GroupItem -> {
                    GroupCellModel(
                        id = item.group.uid.toString(),
                        title = item.group.title,
                        countText = formatCountsForGroup(item.noteCount, item.childGroupCount)
                    )
                }
                is GroupsInteractor.NoteItem -> {
                    NoteCellModel(
                        id = item.note.uid?.toString() ?: "",
                        title = item.note.title
                    )
                }
                else -> null
            }
        }
    }

    private fun formatCountsForGroup(noteCount: Int, childGroupCount: Int): String {
        return if (noteCount == 0 && childGroupCount == 0) {
            ""
        } else if (noteCount > 0 && childGroupCount == 0) {
            resourceProvider.getString(R.string.notes_with_count, noteCount)
        } else if (noteCount == 0 && childGroupCount > 0) {
            resourceProvider.getString(R.string.groups_with_count, childGroupCount)
        } else {
            resourceProvider.getString(R.string.groups_and_notes_with_count, noteCount, childGroupCount)
        }
    }
}
