package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseConfig

data class KeepassDatabaseConfig(
    override val isRecycleBinEnabled: Boolean
) : EncryptedDatabaseConfig