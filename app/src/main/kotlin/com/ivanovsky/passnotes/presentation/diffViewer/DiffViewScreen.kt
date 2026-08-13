package com.ivanovsky.passnotes.presentation.diffViewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanovsky.passnotes.presentation.core.compose.CenteredBox
import com.ivanovsky.passnotes.presentation.core.compose.EmptyState
import com.ivanovsky.passnotes.presentation.core.compose.ProgressIndicator
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.ErrorPanelCell
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.SpaceCell
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.ui.DiffHeaderCell
import com.ivanovsky.passnotes.presentation.diffViewer.cells.ui.EntryDiffCell
import com.ivanovsky.passnotes.presentation.diffViewer.cells.ui.GroupDiffCell
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.DiffHeaderCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.EntryDiffCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.GroupDiffCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.model.DiffViewerState

@Composable
fun DiffViewerScreen(
    viewModel: DiffViewerViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DiffViewerScreen(
        state = state
    )
}

@Composable
private fun DiffViewerScreen(state: DiffViewerState) {
    when (state) {
        is DiffViewerState.Loading -> ProgressIndicator()
        is DiffViewerState.Empty -> EmptyState(message = state.message)

        is DiffViewerState.Error -> {
            CenteredBox {
                ErrorPanelCell(state.cellViewModel)
            }
        }

        is DiffViewerState.Data -> {
            Column {
                if (state.errorCellViewModel != null) {
                    ErrorPanelCell(state.errorCellViewModel)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.viewModels) { viewModel ->
                        CreateCell(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateCell(viewModel: CellViewModel) {
    when (viewModel) {
        is DiffHeaderCellViewModel -> DiffHeaderCell(viewModel)
        is EntryDiffCellViewModel -> EntryDiffCell(viewModel)
        is GroupDiffCellViewModel -> GroupDiffCell(viewModel)
        is SpaceCellViewModel -> SpaceCell(viewModel)
    }
}

// TODO: add screen preview