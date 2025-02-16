package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ivanovsky.passnotes.R
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

@Composable
fun ElementSpace() {
    Spacer(modifier = Modifier.height(height = ElementMargin))
}

@Composable
fun shortDummyText(): String =
    stringResource(R.string.short_dummy_text)

@Composable
fun mediumDummyText(): String =
    stringResource(R.string.medium_dummy_text)

@Composable
fun veryLongDummyText(): String =
    stringResource(R.string.long_dummy_text)

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