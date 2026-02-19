package com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

data class CheckboxCellModel(
    override val id: String,
    val title: String,
    val description: String,
    val isChecked: Boolean
) : BaseCellModel(id)