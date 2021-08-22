package com.ivanovsky.passnotes.presentation.note.factory

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.presentation.core.factory.CellModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.NotePropertyCellModel
import com.ivanovsky.passnotes.util.StringUtils

class NoteCellModelFactory : CellModelFactory<List<Property>> {

    override fun createCellModels(data: List<Property>): List<BaseCellModel> {
        return data.map { property ->
            NotePropertyCellModel(
                id = property.name ?: StringUtils.EMPTY,
                name = property.name ?: StringUtils.EMPTY,
                value = property.value ?: StringUtils.EMPTY,
                isValueHidden = property.isProtected,
                isVisibilityButtonVisible = property.isProtected,
                isCopyButtonVisible = true
            )
        }
    }
}