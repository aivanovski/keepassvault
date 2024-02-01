package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ThemedScreenPreview(
    theme: ComposeTheme,
    content: @Composable () -> Unit
) {
    AppTheme(
        theme = theme
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = AppTheme.theme.colors.background
                )
        ) {
            content.invoke()
        }
    }
}