package com.ivanovsky.passnotes.presentation.syncState.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

data class SyncStateModel(
    val message: String,
    val messageColor: Int,
    val isProgressVisible: Boolean,
    val isMessageDismissed: Boolean,
    val buttonAction: ButtonAction = ButtonAction.NONE
) : BaseCellModel()