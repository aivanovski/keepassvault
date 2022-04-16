package com.ivanovsky.passnotes.presentation.search.factory

import com.ivanovsky.passnotes.domain.interactor.search.SearchInteractor.Item
import com.ivanovsky.passnotes.presentation.core.factory.CellModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.GroupCellModel
import com.ivanovsky.passnotes.presentation.core.model.NoteCellModel

class SearchCellModelFactory : CellModelFactory<List<Item>> {

    override fun createCellModels(data: List<Item>): List<BaseCellModel> {
        return data.map { item ->
            when (item) {
                is Item.NoteItem -> {
                    NoteCellModel(
                        id = item.note.uid?.toString() ?: "",
                        note = item.note
                    )
                }
                is Item.GroupItem -> {
                    GroupCellModel(
                        id = item.group.uid.toString(),
                        group = item.group,
                        noteCount = item.group.noteCount,
                        childGroupCount = item.group.groupCount
                    )
                }
            }
        }
    }
}