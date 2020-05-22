package com.ivanovsky.passnotes.domain.globalsnackbar

import com.ivanovsky.passnotes.presentation.Screen

class SnackbarReceiverFilter private constructor(private val exclude: List<Screen>) {

    fun isAcceptable(screen: Screen): Boolean {
        return !exclude.contains(screen)
    }

    companion object {

        fun allExceptCurrentScreen(current: Screen): SnackbarReceiverFilter {
            return SnackbarReceiverFilter(listOf(current))
        }
    }
}
