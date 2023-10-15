package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey

data class PasswordKeepassKey(
    val password: String
) : EncryptedDatabaseKey {

    override val type: KeyType = KeyType.PASSWORD
}