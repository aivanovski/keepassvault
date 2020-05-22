package com.ivanovsky.passnotes.domain.globalsnackbar

import com.ivanovsky.passnotes.presentation.core.SnackbarMessage

class GlobalSnackbarBus {

    val messageAction = GlobalSnackbarMessageLiveAction()

    fun send(message: SnackbarMessage, filter: SnackbarReceiverFilter) {
        messageAction.value = FilterableSnackbarMessage(message, filter)
    }
}