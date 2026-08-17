package com.ivanovsky.passnotes.data.entity

enum class SyncResolution {
    UPLOAD_LOCAL,
    DOWNLOAD_REMOTE,
    CONFLICT,
    NO_CHANGES
}