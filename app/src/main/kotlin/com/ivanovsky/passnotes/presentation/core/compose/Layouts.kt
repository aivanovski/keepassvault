package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CenteredBox(content: @Composable BoxScope.() -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
        content = content
    )
}

@Composable
@Preview
fun CenteredBoxPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        CenteredBox {
            Text("In the center")
        }
    }
}