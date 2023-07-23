package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.SyncStatus

fun SyncStatus.isNeedToSync(): Boolean {
    return this == SyncStatus.LOCAL_CHANGES ||
        this == SyncStatus.LOCAL_CHANGES_NO_NETWORK ||
        this == SyncStatus.REMOTE_CHANGES
}

fun SyncStatus.hasLocalChanges(): Boolean {
    return this == SyncStatus.LOCAL_CHANGES ||
        this == SyncStatus.LOCAL_CHANGES_NO_NETWORK
}