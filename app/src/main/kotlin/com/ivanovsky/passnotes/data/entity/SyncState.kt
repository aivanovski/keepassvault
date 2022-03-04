package com.ivanovsky.passnotes.data.entity

data class SyncState(
    val status: SyncStatus,
    val progress: SyncProgressStatus
)