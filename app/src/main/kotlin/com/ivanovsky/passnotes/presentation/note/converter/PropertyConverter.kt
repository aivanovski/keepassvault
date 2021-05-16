package com.ivanovsky.passnotes.presentation.note.converter

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.NotePropertyCellModel
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

fun List<Property>.toCellModels(): List<BaseCellModel> {
    return map { property ->
        NotePropertyCellModel(
            id = property.name ?: EMPTY,
            name = property.name ?: EMPTY,
            value = property.value ?: EMPTY,
            isValueHidden = property.isProtected,
            isVisibilityButtonVisible = property.isProtected,
            isCopyButtonVisible = true
        )
    }
}