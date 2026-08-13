package com.ivanovsky.passnotes.presentation.history.cells.model

import androidx.compose.runtime.Immutable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellModel

@Immutable
data class HistoryDiffPlaceholderCellModel(
    override val id: Int,
    val title: String
) : CellModel