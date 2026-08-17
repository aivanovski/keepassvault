package com.ivanovsky.passnotes.presentation.history.model

import androidx.compose.runtime.Immutable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.ErrorPanelCellViewModel

@Immutable
sealed interface HistoryState {

    @Immutable
    data object Loading : HistoryState

    @Immutable
    data object Empty : HistoryState

    @Immutable
    data class Error(
        val cellViewModel: ErrorPanelCellViewModel
    ) : HistoryState

    @Immutable
    data class Data(
        val viewModels: List<CellViewModel>
    ) : HistoryState
}