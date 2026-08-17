package com.ivanovsky.passnotes.presentation.diffViewer.model

import androidx.compose.runtime.Immutable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.ErrorPanelCellViewModel

@Immutable
interface DiffViewerState {

    @Immutable
    data object Loading : DiffViewerState

    @Immutable
    data class Empty(
        val message: String
    ) : DiffViewerState

    @Immutable
    data class Error(
        val cellViewModel: ErrorPanelCellViewModel
    ) : DiffViewerState

    @Immutable
    data class Data(
        val viewModels: List<CellViewModel>,
        val errorCellViewModel: ErrorPanelCellViewModel? = null
    ) : DiffViewerState
}