package com.ivanovsky.passnotes.data.repository.encdb

import java.util.UUID

interface EncryptedDatabaseConfig {
    val isRecycleBinEnabled: Boolean
    val recycleBinUid: UUID?
    val maxHistoryItems: Int
    fun toMutableConfig(): MutableEncryptedDatabaseConfig
}