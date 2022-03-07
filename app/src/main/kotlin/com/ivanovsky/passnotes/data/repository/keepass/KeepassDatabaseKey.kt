package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey

data class KeepassDatabaseKey(
    private val password: String
) : EncryptedDatabaseKey {

    override val key: ByteArray
        get() = password.toByteArray()
}