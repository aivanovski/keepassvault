package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey

data class FileKeepassKey(
    val file: FileDescriptor,
    val password: String? = null
) : EncryptedDatabaseKey {

    override val type: KeyType = KeyType.KEY_FILE
}