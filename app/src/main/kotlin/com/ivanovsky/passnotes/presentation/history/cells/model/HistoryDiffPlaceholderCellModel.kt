package com.ivanovsky.passnotes.presentation.history.cells.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

data class HistoryDiffPlaceholderCellModel(
    override val id: Int,
    val title: String
) : BaseCellModel(id)