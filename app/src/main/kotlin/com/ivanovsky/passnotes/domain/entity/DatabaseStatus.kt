package com.ivanovsky.passnotes.domain.entity

enum class DatabaseStatus {
    NORMAL,
    READ_ONLY,
    CACHED,
    DELAYED_CHANGES
}