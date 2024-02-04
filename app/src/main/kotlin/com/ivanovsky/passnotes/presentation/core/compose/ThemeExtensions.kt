package com.ivanovsky.passnotes.presentation.core.compose

import android.content.Context
import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import com.ivanovsky.passnotes.util.ThemeUtils

fun Context.getComposeTheme(): ComposeTheme {
    val isNightMode = ThemeUtils.isNightMode(this)

    return if (isNightMode) {
        DarkTheme
    } else {
        LightTheme
    }
}

fun ThemeProvider.Theme.toComposeTheme(): ComposeTheme {
    return when (this) {
        ThemeProvider.Theme.LIGHT -> LightTheme
        ThemeProvider.Theme.DARK -> DarkTheme
    }
}