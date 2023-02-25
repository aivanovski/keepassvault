package com.ivanovsky.passnotes.presentation.noteEditor.cells.model

import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.widget.entity.TextInputLines
import com.ivanovsky.passnotes.presentation.core.widget.entity.TextInputType

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