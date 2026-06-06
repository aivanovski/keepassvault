package com.ivanovsky.passnotes.data.repository.encdb

import java.util.UUID

data class MutableEncryptedDatabaseConfig(
    override var isRecycleBinEnabled: Boolean,
    override val recycleBinUid: UUID?,
    override var maxHistoryItems: Int
) : EncryptedDatabaseConfig {

    override fun toMutableConfig(): MutableEncryptedDatabaseConfig = this
}