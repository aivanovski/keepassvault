package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey

data class PasswordKeepassKey(
    private val password: String
) : EncryptedDatabaseKey {

    override val type: KeyType
        get() = KeyType.PASSWORD

    override fun getKey(): OperationResult<ByteArray> {
        return OperationResult.success(password.toByteArray())
    }
}