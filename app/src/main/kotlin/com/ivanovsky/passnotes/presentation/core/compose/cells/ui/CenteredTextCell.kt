package com.ivanovsky.passnotes.presentation.core.compose.cells.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.ComposeTheme
import com.ivanovsky.passnotes.presentation.core.compose.ComposeThemeProvider
import com.ivanovsky.passnotes.presentation.core.compose.PrimaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.ThemedScreenPreview
import com.ivanovsky.passnotes.presentation.core.compose.TwoLineListItemHeight
import com.ivanovsky.passnotes.presentation.core.compose.cells.model.CenteredTextCellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.toId
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.CenteredTextCellViewModel

@Composable
fun CenteredTextCell(viewModel: CenteredTextCellViewModel) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = viewModel.model.height)
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
private fun CenteredTextCellPreview(
    @PreviewParameter(ComposeThemeProvider::class) theme: ComposeTheme
) {
    ThemedScreenPreview(theme) {
        CenteredTextCell(newCenteredTextCell())
    }
}

@Composable
fun newCenteredTextCell() =
    CenteredTextCellViewModel(
        model = CenteredTextCellModel(
            id = 1.toId(),
            title = stringResource(R.string.no_changes),
            height = TwoLineListItemHeight
        )
    )