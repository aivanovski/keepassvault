package com.ivanovsky.passnotes.presentation.core.navigation

sealed interface NavigationEvent {

    data class SetRoot(
        val screens: List<Screen>
    ) : NavigationEvent

    data class NavigateTo(
        val screen: Screen
    ) : NavigationEvent

    data class BackTo(
        val screen: Screen
    ) : NavigationEvent

    data class ReplaceCurrent(
        val screen: Screen
    ) : NavigationEvent

    data object Back : NavigationEvent
}