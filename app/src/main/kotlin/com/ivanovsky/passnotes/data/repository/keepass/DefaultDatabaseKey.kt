package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey

class DefaultDatabaseKey : EncryptedDatabaseKey {
    override val key: ByteArray
        get() = throw IllegalStateException()
}