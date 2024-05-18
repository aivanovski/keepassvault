package com.ivanovsky.passnotes.presentation.history.cells.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.DarkTheme
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.PrimaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.ThemedScreenPreview
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryDiffPlaceholderCellModel
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryDiffPlaceholderCellViewModel

@Composable
fun HistoryDiffPlaceholderCell(viewModel: HistoryDiffPlaceholderCellViewModel) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 70.dp)
    ) {
        Text(
            text = viewModel.model.title,
            style = PrimaryTextStyle(),
            modifier = Modifier
        )
    }
}

@Composable
@Preview
fun PlaceholderLightPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        HistoryDiffPlaceholderCell(newViewModel(newDiffPlaceholderModel()))
    }
}

@Composable
@Preview
fun PlaceholderDarkPreview() {
    ThemedScreenPreview(theme = DarkTheme) {
        HistoryDiffPlaceholderCell(newViewModel(newDiffPlaceholderModel()))
    }
}

@Composable
fun newDiffPlaceholderModel() =
    HistoryDiffPlaceholderCellModel(
        id = 1,
        title = stringResource(R.string.no_changes)
    )

private fun newViewModel(model: HistoryDiffPlaceholderCellModel) =
    HistoryDiffPlaceholderCellViewModel(model)