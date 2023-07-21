package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.domain.ResourceProvider

fun SyncState.getLockServiceMessage(resourceProvider: ResourceProvider): String {
    return when (status) {
        SyncStatus.NO_CHANGES -> {
            resourceProvider.getString(R.string.lock_notification_text_normal)
        }
        else -> {
            resourceProvider.getString(R.string.lock_notification_text_not_synchronized)
        }
    }
}