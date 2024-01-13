package com.ivanovsky.passnotes.data.repository.encdb

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.KeyType

interface EncryptedDatabaseKey : Parcelable {
    val type: KeyType
}