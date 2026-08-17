package com.ivanovsky.passnotes.presentation.history

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
import com.ivanovsky.passnotes.presentation.core.compose.EmptyState
import com.ivanovsky.passnotes.presentation.core.compose.ProgressIndicator
import com.ivanovsky.passnotes.presentation.core.compose.ThemedScreenPreview
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.CenteredTextCell
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.DividerCell
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.ErrorPanelCell
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.newCenteredTextCell
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.newErrorStateCell
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.CenteredTextCellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.DividerCellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.ui.HistoryDiffCell
import com.ivanovsky.passnotes.presentation.history.cells.ui.HistoryHeaderCell
import com.ivanovsky.passnotes.presentation.history.cells.ui.newHistoryDeleteCell
import com.ivanovsky.passnotes.presentation.history.cells.ui.newHistoryHeaderCell
import com.ivanovsky.passnotes.presentation.history.cells.ui.newHistoryInsertCell
import com.ivanovsky.passnotes.presentation.history.cells.ui.newHistoryUpdateCell
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryDiffCellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryHeaderCellViewModel
import com.ivanovsky.passnotes.presentation.history.model.HistoryState

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HistoryScreen(state = state)
}

@Composable
private fun HistoryScreen(
    state: HistoryState
) {
    when (state) {
        is HistoryState.Loading -> {
            ProgressIndicator()
        }

        is HistoryState.Empty -> {
            EmptyState(
                message = stringResource(R.string.no_items)
            )
        }

        is HistoryState.Error -> {
            CenteredBox {
                ErrorPanelCell(state.cellViewModel)
            }
        }

        is HistoryState.Data -> {
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

@Composable
private fun CreateCell(viewModel: CellViewModel) {
    when (viewModel) {
        is HistoryHeaderCellViewModel -> HistoryHeaderCell(viewModel)
        is HistoryDiffCellViewModel -> HistoryDiffCell(viewModel)
        is CenteredTextCellViewModel -> CenteredTextCell(viewModel)
        is DividerCellViewModel -> DividerCell(viewModel)
    }
}

@Preview
@Composable
fun HistoryPreview_Data(
    @PreviewParameter(ComposeThemeProvider::class) theme: ComposeTheme
) {
    ThemedScreenPreview(theme) {
        HistoryScreen(state = newDataState())
    }
}

@Preview
@Composable
fun HistoryPreview_Error(
    @PreviewParameter(ComposeThemeProvider::class) theme: ComposeTheme
) {
    ThemedScreenPreview(theme) {
        HistoryScreen(
            state = HistoryState.Error(
                cellViewModel = newErrorStateCell()
            )
        )
    }
}

@Preview
@Composable
fun HistoryPreview_Empty(
    @PreviewParameter(ComposeThemeProvider::class) theme: ComposeTheme
) {
    ThemedScreenPreview(theme) {
        HistoryScreen(state = HistoryState.Empty)
    }
}

@Preview
@Composable
fun HistoryPreview_Loading(
    @PreviewParameter(ComposeThemeProvider::class) theme: ComposeTheme
) {
    ThemedScreenPreview(theme) {
        HistoryScreen(state = HistoryState.Loading)
    }
}

@Composable
private fun newDataState() =
    HistoryState.Data(
        viewModels = listOf(
            newHistoryHeaderCell(),
            newHistoryInsertCell(),
            newHistoryHeaderCell(),
            newHistoryDeleteCell(),
            newHistoryHeaderCell(),
            newHistoryUpdateCell(),
            newHistoryHeaderCell(),
            newCenteredTextCell(),
            newHistoryHeaderCell()
        )
    )