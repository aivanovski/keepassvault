package com.ivanovsky.passnotes.data.repository.encdb

import com.ivanovsky.passnotes.data.entity.KeyType

interface EncryptedDatabaseKey {
    val type: KeyType
}