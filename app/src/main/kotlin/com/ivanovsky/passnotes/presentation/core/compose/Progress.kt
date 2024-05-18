package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivanovsky.passnotes.R

@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Box(
        modifier = modifier
    ) {
        CircularProgressIndicator(
            color = AppTheme.theme.colors.progress,
            modifier = Modifier
                .size(dimensionResource(R.dimen.progress_bar_size))
                .align(Alignment.Center)
        )
    }
}

@Preview
@Composable
fun LightPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        ProgressIndicator()
    }
}

@Preview
@Composable
fun DarkPreview() {
    ThemedScreenPreview(theme = DarkTheme) {
        ProgressIndicator()
    }
}