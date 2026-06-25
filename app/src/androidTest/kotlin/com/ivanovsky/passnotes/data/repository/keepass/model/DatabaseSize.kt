package com.ivanovsky.passnotes.data.repository.keepass.model

enum class DatabaseSize(val sizeInBytes: Long) {
    SMALL(sizeInBytes = 1 * 1024), // 1KB
    MEDIUM(sizeInBytes = 50 * 1024), // 50 KB
    LARGE(sizeInBytes = 5 * 1024 * 1024) // 5 MB
}