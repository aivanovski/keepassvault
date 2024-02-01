package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class KpassnotesTypography(
    val header: TextStyle,
    val primary: TextStyle,
    val secondary: TextStyle
)

val AppTypography = KpassnotesTypography(
    header = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    ),
    primary = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    secondary = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    )
)