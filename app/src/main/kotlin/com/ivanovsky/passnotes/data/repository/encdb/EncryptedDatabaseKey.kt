package com.ivanovsky.passnotes.data.repository.encdb

import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.entity.OperationResult

interface EncryptedDatabaseKey {
    val type: KeyType
    fun getKey(): OperationResult<ByteArray>
}