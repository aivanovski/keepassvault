package com.ivanovsky.passnotes.presentation.note_editor.cells.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.widget.entity.TextInputType

data class ExtendedTextPropertyCellModel(
    override val id: String,
    val name: String,
    val value: String,
    val isProtected: Boolean,
    val isCollapsed: Boolean,
    val inputType: TextInputType
) : BaseCellModel()