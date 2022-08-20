package com.ivanovsky.passnotes.data.repository.encdb

data class MutableEncryptedDatabaseConfig(
    override var isRecycleBinEnabled: Boolean
) : EncryptedDatabaseConfig {

    override fun toMutableConfig(): MutableEncryptedDatabaseConfig = this
}