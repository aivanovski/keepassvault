package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Immutable
data class ComposeTheme(
    val colors: AppColors,
    val materialColors: ColorScheme,
    val textMetrics: TextMetrics
)

@Immutable
data class TextMetrics(
    val primary: TextUnit = 16.sp,
    val secondary: TextUnit = 14.sp,
    val header: TextUnit = 22.sp,
    val small: TextUnit = 12.sp
)

val LightTheme = ComposeTheme(
    colors = LightAppColors,
    materialColors = lightColorScheme(
        primary = LightAppColors.primary,
        onPrimary = LightAppColors.primaryText,
        primaryContainer = LightAppColors.fabColor,
        onPrimaryContainer = LightAppColors.fabText,
        surface = LightAppColors.surface,
        onSurface = LightAppColors.primaryText,
        outline = LightAppColors.outline,
        errorContainer = LightAppColors.errorBackground,
        onError = LightAppColors.errorText,
        error = LightAppColors.errorText,
        background = LightAppColors.background
    ),
    textMetrics = TextMetrics()
)

val DarkTheme = ComposeTheme(
    colors = DarkAppColors,
    materialColors = darkColorScheme(
        primary = DarkAppColors.primary,
        onPrimary = DarkAppColors.primaryText,
        primaryContainer = DarkAppColors.fabColor,
        onPrimaryContainer = DarkAppColors.fabText,
        surface = DarkAppColors.surface,
        onSurface = DarkAppColors.primaryText,
        outline = DarkAppColors.outline,
        errorContainer = DarkAppColors.errorBackground,
        onError = DarkAppColors.errorText,
        error = DarkAppColors.errorText,
        background = DarkAppColors.background
    ),
    textMetrics = TextMetrics()
)

val LocalExtendedColors = staticCompositionLocalOf {
    LightTheme
}

@Composable
fun AppTheme(
    theme: ComposeTheme,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalExtendedColors provides theme) {
        MaterialTheme(
            colorScheme = theme.materialColors,
            content = content
        )
    }
}

object AppTheme {
    val theme: ComposeTheme
        @Composable
        get() = LocalExtendedColors.current
}