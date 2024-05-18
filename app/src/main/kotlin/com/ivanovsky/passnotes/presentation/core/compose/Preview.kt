package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.event.Event
import com.ivanovsky.passnotes.presentation.core.event.EventProvider

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

@Composable
fun ThemedPreview(
    theme: ComposeTheme,
    content: @Composable () -> Unit
) {
    AppTheme(
        theme = theme
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppTheme.theme.colors.background
                )
        ) {
            content.invoke()
        }
    }
}

fun newEventProvider(): EventProvider {
    return object : EventProvider {
        override fun subscribe(subscriber: Any, observer: (event: Event) -> Unit) {
        }

        override fun unSubscribe(subscriber: Any) {
        }

        override fun send(event: Event) {
        }

        override fun clear() {
        }
    }
}

@Composable
fun newResourceProvider(): ResourceProvider {
    return ResourceProvider(LocalContext.current)
}