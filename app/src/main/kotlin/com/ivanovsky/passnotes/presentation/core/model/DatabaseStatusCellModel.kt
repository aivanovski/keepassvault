package com.ivanovsky.passnotes.presentation.core.model

data class DatabaseStatusCellModel(
    val text: String,
    val isVisible: Boolean
) : BaseCellModel()