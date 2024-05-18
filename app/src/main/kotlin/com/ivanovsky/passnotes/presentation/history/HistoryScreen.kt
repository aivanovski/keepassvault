package com.ivanovsky.passnotes.presentation.history

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.EmptyState
import com.ivanovsky.passnotes.presentation.core.compose.ErrorState
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.ProgressIndicator
import com.ivanovsky.passnotes.presentation.core.compose.ThemedScreenPreview
import com.ivanovsky.passnotes.presentation.core.compose.newEventProvider
import com.ivanovsky.passnotes.presentation.core.compose.newResourceProvider
import com.ivanovsky.passnotes.presentation.history.cells.CellFactory
import com.ivanovsky.passnotes.presentation.history.cells.ui.newDeleteModel
import com.ivanovsky.passnotes.presentation.history.cells.ui.newHistoryHeaderModel
import com.ivanovsky.passnotes.presentation.history.cells.ui.newInsertModel
import com.ivanovsky.passnotes.presentation.history.cells.ui.newUpdateModel
import com.ivanovsky.passnotes.presentation.history.factory.HistoryCellViewModelFactory
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
    val cellFactory = CellFactory()

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
            ErrorState(
                message = state.message
            )
        }

        is HistoryState.Data -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.viewModels) { viewModel ->
                    cellFactory.createCell(viewModel)
                }
            }
        }
    }
}

@Preview
@Composable
fun LightPreviewWithLoading() {
    ThemedScreenPreview(theme = LightTheme) {
        HistoryScreen(state = HistoryState.Loading)
    }
}

@Preview
@Composable
fun LightPreviewWithEmptyState() {
    ThemedScreenPreview(theme = LightTheme) {
        HistoryScreen(state = HistoryState.Empty)
    }
}

@Preview
@Composable
fun LightPreviewWithError() {
    ThemedScreenPreview(theme = LightTheme) {
        HistoryScreen(
            state = HistoryState.Error(
                message = stringResource(R.string.error_has_been_occurred)
            )
        )
    }
}

@Preview
@Composable
fun LightPreviewWithData() {
    val factory = HistoryCellViewModelFactory(newResourceProvider())

    val models = listOf(
        newHistoryHeaderModel(),
        newInsertModel(),
        newHistoryHeaderModel(),
        newDeleteModel(),
        newHistoryHeaderModel(),
        newUpdateModel(),
        newHistoryHeaderModel(title = "Created 01.01.2024 12:00:00")
    )

    val state = HistoryState.Data(
        viewModels = factory.createCellViewModels(models, newEventProvider())
    )

    ThemedScreenPreview(theme = LightTheme) {
        HistoryScreen(state = state)
    }
}