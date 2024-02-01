package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class KpassnotesColors(
    val primary: Color,
    val background: Color,
    val actionBarBackground: Color,
    val dialogBackground: Color,
    val dialogPrimary: Color,
    val selectedBackground: Color,
    val surface: Color,
    val errorBackground: Color,
    val divider: Color,
    val fabColor: Color,
    val outline: Color,
    val fabText: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val errorText: Color,
    val actionBarText: Color,
    val hyperlinkText: Color,
    val primaryIcon: Color,
    val secondaryIcon: Color,
    val secondaryIconBackground: Color,
    val importantIcon: Color,
    val diffInsert: Color,
    val diffDelete: Color,
    val diffUpdate: Color
)

val LightAppColors = KpassnotesColors(
    primary = Color(0xFF3F51B5),
    background = Color(0xFFF6F7FB),
    actionBarBackground = Color(0xFF3F51B5),
    dialogBackground = Color(0xFFF6F7FB),
    dialogPrimary = Color(0xFF3F51B5),
    selectedBackground = Color(0xFFDEE1F9),
    surface = Color(0xFFFFFFFF),
    errorBackground = Color(0xFFFFF2F0),
    divider = Color(0xFFE5E8F1),
    fabColor = Color(0xFFEADDFF),
    outline = Color(0xFFE3E3E4),
    fabText = Color(0xFF282E3E),
    primaryText = Color(0xFF282E3E),
    secondaryText = Color(0xFF586380),
    errorText = Color(0xFFC00020),
    actionBarText = Color(0xFFFFFFFF),
    hyperlinkText = Color(0xFF64B5F6),
    primaryIcon = Color(0xFF586380),
    secondaryIcon = Color(0xFFFFFFFF),
    secondaryIconBackground = Color(0xFFD9DDE8),
    importantIcon = Color(0xFFEF5350),
    diffInsert = Color(0xFFBEFFBB),
    diffDelete = Color(0xFFFFD5D5),
    diffUpdate = Color(0xFFF0EFAA)
)

val DarkAppColors = KpassnotesColors(
    primary = Color(0xFF2E3856),
    background = Color(0xFF0A092D),
    actionBarBackground = Color(0xFF0A092D),
    dialogBackground = Color(0xFF0A092D),
    dialogPrimary = Color(0xFFA8B1FF),
    selectedBackground = Color(0xFF35446F),
    surface = Color(0xFF2E3856),
    errorBackground = Color(0xFFFFB4AB),
    divider = Color(0xFF282E3E),
    fabColor = Color(0xFFEADDFF),
    outline = Color(0xFF586380),
    fabText = Color(0xFF282E3E),
    primaryText = Color(0xFFF6F7FB),
    secondaryText = Color(0xFFD9DDE8),
    errorText = Color(0xFFD3555B),
    actionBarText = Color(0xFFF6F7FB),
    hyperlinkText = Color(0xFF64B5F6),
    primaryIcon = Color(0xFF586380),
    secondaryIcon = Color(0xFF0A092D),
    secondaryIconBackground = Color(0xFF586380),
    importantIcon = Color(0xFF690005),
    diffInsert = Color(0xFF061E0B),
    diffDelete = Color(0xFF300406),
    diffUpdate = Color(0xFF33331B)
)