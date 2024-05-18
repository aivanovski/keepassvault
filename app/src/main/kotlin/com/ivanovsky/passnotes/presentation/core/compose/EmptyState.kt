package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivanovsky.passnotes.R

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = PrimaryTextStyle()
        )
    }
}

@Composable
@Preview
fun DarkEmptyStatePreview() {
    ThemedScreenPreview(theme = DarkTheme) {
        EmptyState(message = stringResource(R.string.no_items))
    }
}

@Composable
@Preview
fun LightEmptyStatePreview() {
    ThemedScreenPreview(theme = LightTheme) {
        EmptyState(message = stringResource(R.string.no_items))
    }
}