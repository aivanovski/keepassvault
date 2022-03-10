package com.ivanovsky.passnotes.data.entity

enum class SyncStatus {
    NO_CHANGES,
    LOCAL_CHANGES,
    LOCAL_CHANGES_NO_NETWORK,
    REMOTE_CHANGES,
    NO_NETWORK,
    ERROR,
    CONFLICT
}