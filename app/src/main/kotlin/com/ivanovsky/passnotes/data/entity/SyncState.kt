package com.ivanovsky.passnotes.data.entity

data class SyncState(
    val status: SyncStatus,
    val progress: SyncProgressStatus,
    val revision: String?
) {

    companion object {
        val DEFAULT = SyncState(
            status = SyncStatus.NO_CHANGES,
            progress = SyncProgressStatus.IDLE,
            revision = null
        )
    }
}