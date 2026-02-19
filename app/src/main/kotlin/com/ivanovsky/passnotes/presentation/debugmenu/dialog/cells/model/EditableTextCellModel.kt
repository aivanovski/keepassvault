package com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

data class EditableTextCellModel(
    override val id: String,
    val text: String,
    val hint: String
) : BaseCellModel(id)