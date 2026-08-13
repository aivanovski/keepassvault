package com.ivanovsky.passnotes.presentation.history.cells.viewModel

import androidx.compose.runtime.Immutable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryDiffPlaceholderCellModel

@Immutable
class HistoryDiffPlaceholderCellViewModel(
    override val model: HistoryDiffPlaceholderCellModel
) : CellViewModel