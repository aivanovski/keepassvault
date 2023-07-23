package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.SyncProgressStatus

fun SyncProgressStatus.isIdle(): Boolean {
    return this == SyncProgressStatus.IDLE
}

fun SyncProgressStatus.isSyncInProgress(): Boolean {
    return !this.isIdle()
}