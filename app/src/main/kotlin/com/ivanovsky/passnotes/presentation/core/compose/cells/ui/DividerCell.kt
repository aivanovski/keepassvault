package com.ivanovsky.passnotes.presentation.core.compose.cells.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.DarkTheme
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.ThemedPreview
import com.ivanovsky.passnotes.presentation.core.compose.newResourceProvider
import com.ivanovsky.passnotes.presentation.core.model.DividerCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.DividerCellViewModel

@Composable
fun DividerCell(viewModel: DividerCellViewModel) {
    val paddingStart = if (viewModel.model.paddingStart != null) {
        dimensionResource(viewModel.model.paddingStart)
    } else {
        0.dp
    }

    val paddingEnd = if (viewModel.model.paddingEnd != null) {
        dimensionResource(viewModel.model.paddingEnd)
    } else {
        0.dp
    }

    HorizontalDivider(
        thickness = 1.dp,
        color = Color(viewModel.model.color),
        modifier = Modifier
            .padding(
                start = paddingStart,
                end = paddingEnd
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
            color = AppTheme.theme.colors.divider.toArgb(),
            paddingStart = R.dimen.element_margin,
            paddingEnd = R.dimen.element_margin
        ),
        resourceProvider = newResourceProvider()
    )