package com.ivanovsky.passnotes.presentation.syncState.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

data class SyncStateModel(
    val message: String,
    val detailsMessage: String,
    val messageColor: Int,
    val isSyncIconVisible: Boolean,
    val isMessageDismissed: Boolean,
    val buttonAction: ButtonAction = ButtonAction.NONE
) : BaseCellModel()