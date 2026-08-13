package com.ivanovsky.passnotes.presentation.core.compose.cells.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.DarkTheme
import com.ivanovsky.passnotes.presentation.core.compose.ElementMargin
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.ThemedPreview
import com.ivanovsky.passnotes.presentation.core.compose.cells.model.DividerCellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.toId
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.DividerCellViewModel

@Composable
fun DividerCell(viewModel: DividerCellViewModel) {
    HorizontalDivider(
        thickness = 1.dp,
        color = viewModel.model.color,
        modifier = Modifier
            .padding(
                start = viewModel.model.paddingStart,
                end = viewModel.model.paddingEnd
            )
    )
}

@Composable
@Preview
fun LightDividerPreview() {
    ThemedPreview(theme = LightTheme) {
        Column {
            Box(modifier = Modifier.height(10.dp))
            DividerCell(newDividerViewModel())
            Box(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
@Preview
fun DakrDividerPreview() {
    ThemedPreview(theme = DarkTheme) {
        Column {
            Box(modifier = Modifier.height(10.dp))
            DividerCell(newDividerViewModel())
            Box(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun newDividerViewModel() =
    DividerCellViewModel(
        model = DividerCellModel(
            id = 1.toId(),
            color = AppTheme.theme.colors.divider,
            paddingStart = ElementMargin,
            paddingEnd = ElementMargin
        )
    )