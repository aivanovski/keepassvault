package com.ivanovsky.passnotes.presentation.core.compose

import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun themeFlow(themeProvider: ThemeProvider): Flow<ComposeTheme> =
    callbackFlow {
        val listener: (theme: ThemeProvider.Theme) -> Unit = { theme ->
            trySend(theme.toComposeTheme())
        }

        val currentTheme = themeProvider.getTheme()?.toComposeTheme()
        if (currentTheme != null) {
            trySend(currentTheme)
        }

        themeProvider.subscribe(listener)

        awaitClose { themeProvider.unsubscribe(listener) }
    }