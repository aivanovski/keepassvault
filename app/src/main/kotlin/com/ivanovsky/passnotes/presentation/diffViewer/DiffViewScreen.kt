package com.ivanovsky.passnotes.presentation.diffViewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.CenteredBox
import com.ivanovsky.passnotes.presentation.core.compose.ComposeTheme
import com.ivanovsky.passnotes.presentation.core.compose.ComposeThemeProvider
import com.ivanovsky.passnotes.presentation.core.compose.DoubleElementMargin
import com.ivanovsky.passnotes.presentation.core.compose.EmptyState
import com.ivanovsky.passnotes.presentation.core.compose.ProgressIndicator
import com.ivanovsky.passnotes.presentation.core.compose.ThemedScreenPreview
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.ErrorPanelCell
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.SpaceCell
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.newErrorPanelCell
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.newErrorStateCell
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.newSpaceCell
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.ui.DiffHeaderCell
import com.ivanovsky.passnotes.presentation.diffViewer.cells.ui.EntryDiffCell
import com.ivanovsky.passnotes.presentation.diffViewer.cells.ui.GroupDiffCell
import com.ivanovsky.passnotes.presentation.diffViewer.cells.ui.newDeleteEntryCell
import com.ivanovsky.passnotes.presentation.diffViewer.cells.ui.newDeleteGroupCell
import com.ivanovsky.passnotes.presentation.diffViewer.cells.ui.newDiffHeaderCell
import com.ivanovsky.passnotes.presentation.diffViewer.cells.ui.newInsertEntryCell
import com.ivanovsky.passnotes.presentation.diffViewer.cells.ui.newInsertGroupCell
import com.ivanovsky.passnotes.presentation.diffViewer.cells.ui.newUpdateEntryCell
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

@Preview
@Composable
fun DiffViewerPreview_Data(
    @PreviewParameter(ComposeThemeProvider::class) theme: ComposeTheme
) {
    ThemedScreenPreview(theme) {
        DiffViewerScreen(
            state = newDataState()
        )
    }
}

@Preview
@Composable
fun DiffViewerPreview_DataWithError(
    @PreviewParameter(ComposeThemeProvider::class) theme: ComposeTheme
) {
    ThemedScreenPreview(theme) {
        DiffViewerScreen(
            state = DiffViewerState.Data(
                viewModels = newDataState().viewModels,
                errorCellViewModel = newErrorPanelCell()
            )
        )
    }
}

@Preview
@Composable
fun DiffViewerPreview_Error(
    @PreviewParameter(ComposeThemeProvider::class) theme: ComposeTheme
) {
    ThemedScreenPreview(theme) {
        DiffViewerScreen(
            state = DiffViewerState.Error(
                cellViewModel = newErrorStateCell()
            )
        )
    }
}

@Preview
@Composable
fun DiffViewerPreview_Empty(
    @PreviewParameter(ComposeThemeProvider::class) theme: ComposeTheme
) {
    ThemedScreenPreview(theme) {
        DiffViewerScreen(
            state = DiffViewerState.Empty(
                message = stringResource(R.string.no_items)
            )
        )
    }
}

@Preview
@Composable
fun DiffViewerPreview_Loading(
    @PreviewParameter(ComposeThemeProvider::class) theme: ComposeTheme
) {
    ThemedScreenPreview(theme) {
        DiffViewerScreen(state = DiffViewerState.Loading)
    }
}

@Composable
private fun newDataState() = DiffViewerState.Data(
    viewModels = listOf(
        newSpaceCell(),
        newDiffHeaderCell(title = stringResource(R.string.local_changes)),
        newSpaceCell(),
        newInsertGroupCell(),
        newSpaceCell(),
        newInsertEntryCell(),
        newSpaceCell(height = DoubleElementMargin),
        newDiffHeaderCell(title = stringResource(R.string.local_changes)),
        newSpaceCell(),
        newDeleteGroupCell(),
        newSpaceCell(),
        newDeleteEntryCell(),
        newSpaceCell(),
        newUpdateEntryCell(),
        newSpaceCell()
    )
)