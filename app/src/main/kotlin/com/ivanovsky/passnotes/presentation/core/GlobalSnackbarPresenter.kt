package com.ivanovsky.passnotes.presentation.core

import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarMessageLiveAction

interface GlobalSnackbarPresenter {

    val globalSnackbarMessageAction: GlobalSnackbarMessageLiveAction
}