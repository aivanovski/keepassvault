package com.ivanovsky.passnotes.data.repository.encdb

interface EncryptedDatabaseConfig {
    val isRecycleBinEnabled: Boolean
    val maxHistoryItems: Int
    fun toMutableConfig(): MutableEncryptedDatabaseConfig
}