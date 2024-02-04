package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class ComposeTheme(
    val colors: KpassnotesColors,
    val materialColors: ColorScheme,
    val typography: KpassnotesTypography
)

val LightTheme = ComposeTheme(
    colors = LightAppColors,
    materialColors = lightColorScheme(),
    typography = AppTypography
)

val DarkTheme = ComposeTheme(
    colors = DarkAppColors,
    materialColors = lightColorScheme(),
    typography = AppTypography
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