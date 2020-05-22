package com.ivanovsky.passnotes.domain.globalsnackbar

import com.ivanovsky.passnotes.presentation.Screen
import com.ivanovsky.passnotes.presentation.core.SnackbarMessage

class FilterableSnackbarMessage(
    message: SnackbarMessage,
    private val filter: SnackbarReceiverFilter
) : SnackbarMessage(message.message, message.isDisplayOkButton) {

    fun isAcceptableForScreen(screen: Screen): Boolean {
        return filter.isAcceptable(screen)
    }
}
