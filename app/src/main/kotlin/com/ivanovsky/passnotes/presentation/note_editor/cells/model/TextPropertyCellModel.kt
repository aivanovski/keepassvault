package com.ivanovsky.passnotes.presentation.note_editor.cells.model

import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.presentation.core_mvvm.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.note_editor.view.TextInputLines
import com.ivanovsky.passnotes.presentation.note_editor.view.TextInputType

data class TextPropertyCellModel(
    override val id: String,
    val name: String,
    val value: String,
    val textInputType: TextInputType,
    val inputLines: TextInputLines,
    val isAllowEmpty: Boolean,
    val propertyType: PropertyType?,
    val propertyName: String?
) : BaseCellModel()