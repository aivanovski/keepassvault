package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ivanovsky.passnotes.R

@Composable
fun ErrorPanel(
    message: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(color = AppTheme.theme.colors.errorBackground)
            .defaultMinSize(minHeight = dimensionResource(R.dimen.error_panel_min_height))
            .padding(
                all = dimensionResource(R.dimen.group_margin)
            )
    ) {
        Text(
            text = message,
            style = ErrorTextStyle(
                isBold = false,
                fontSize = AppTheme.theme.textMetrics.primary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = ErrorTextStyle(),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(
                    horizontal = dimensionResource(R.dimen.group_margin)
                )
        )
    }
}

@Preview
@Composable
fun LightErrorPanelState() {
    ThemedScreenPreview(theme = LightTheme) {
        ErrorPanel(stringResource(R.string.error_offline_mode))
    }
}

@Preview
@Composable
fun DarkErrorPanelState() {
    ThemedScreenPreview(theme = DarkTheme) {
        ErrorPanel(stringResource(R.string.medium_dummy_text))
    }
}

@Preview
@Composable
fun LightErrorState() {
    ThemedScreenPreview(theme = LightTheme) {
        ErrorState(stringResource(R.string.error_offline_mode))
    }
}

@Preview
@Composable
fun DakrErrorState() {
    ThemedScreenPreview(theme = DarkTheme) {
        ErrorState(stringResource(R.string.medium_dummy_text))
    }
}