package com.ivanovsky.passnotes.presentation.unlock.cells.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

data class DatabaseCellModel(
    override val id: String,
    val name: String,
    val path: String,
    val status: String,
    val isStatusVisible: Boolean,
    val isNextButtonVisible: Boolean,
    val onClicked: ((id: String) -> Unit)? = null,
) : BaseCellModel()