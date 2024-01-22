package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class FileKeepassKey(
    val file: FileDescriptor,
    val password: String? = null
) : EncryptedDatabaseKey {

    @IgnoredOnParcel
    override val type: KeyType = KeyType.KEY_FILE
}