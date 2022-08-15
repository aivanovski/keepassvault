package com.ivanovsky.passnotes.utils

import com.ivanovsky.passnotes.data.crypto.DataCipher
import com.ivanovsky.passnotes.data.crypto.DataCipherProvider

class DataCipherProviderImpl(private val cipher: DataCipher) : DataCipherProvider {
    override fun getCipher(): DataCipher = cipher
}