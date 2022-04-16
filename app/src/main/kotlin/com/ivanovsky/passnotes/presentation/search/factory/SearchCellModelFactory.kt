package com.ivanovsky.passnotes.presentation.search.factory

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.presentation.core.factory.CellModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.GroupCellModel
import com.ivanovsky.passnotes.presentation.core.model.NoteCellModel

class SearchCellModelFactory : CellModelFactory<List<EncryptedDatabaseEntry>> {

    override fun createCellModels(data: List<EncryptedDatabaseEntry>): List<BaseCellModel> {
        return data.map { item ->
            when (item) {
                is Note -> {
                    NoteCellModel(
                        id = item.uid?.toString() ?: "",
                        note = item
                    )
                }
                is Group -> {
                    GroupCellModel(
                        id = item.uid.toString(),
                        group = item,
                        noteCount = item.noteCount,
                        childGroupCount = item.groupCount
                    )
                }
                else -> throw IllegalStateException()
            }
        }
    }
}