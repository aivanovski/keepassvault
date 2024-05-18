package com.ivanovsky.passnotes.data.repository.encdb

data class MutableEncryptedDatabaseConfig(
    override var isRecycleBinEnabled: Boolean,
    override var maxHistoryItems: Int
) : EncryptedDatabaseConfig {

    override fun toMutableConfig(): MutableEncryptedDatabaseConfig = this
}