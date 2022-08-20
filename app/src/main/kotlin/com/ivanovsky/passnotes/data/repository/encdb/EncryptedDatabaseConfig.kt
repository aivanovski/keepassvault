package com.ivanovsky.passnotes.data.repository.encdb

interface EncryptedDatabaseConfig {
    val isRecycleBinEnabled: Boolean
    fun toMutableConfig(): MutableEncryptedDatabaseConfig
}