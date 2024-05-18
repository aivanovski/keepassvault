package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
fun LightErrorState() {
    ThemedScreenPreview(theme = LightTheme) {
        ErrorState(stringResource(R.string.error_offline_mode))
    }
}

@Preview
@Composable
fun DakrErrorState() {
    ThemedScreenPreview(theme = DarkTheme) {
        ErrorState("Lorem Ipsum is simply dummy text of the printing and typesetting industry.")
    }
}