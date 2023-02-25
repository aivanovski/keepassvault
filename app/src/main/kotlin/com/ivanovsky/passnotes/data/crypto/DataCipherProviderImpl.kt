package com.ivanovsky.passnotes.data.crypto

import android.content.Context
import com.ivanovsky.passnotes.data.crypto.DataCipherImpl.Companion.isAndroidKeyStoreCipherAllowed
import com.ivanovsky.passnotes.data.crypto.entity.CipherTransformation
import com.ivanovsky.passnotes.data.crypto.keyprovider.FileSecretKeyProvider
import com.ivanovsky.passnotes.data.crypto.keyprovider.KeyStoreSecretKeyProvider

class DataCipherProviderImpl(
    private val context: Context
) : DataCipherProvider {

    private val dataCipher: DataCipher by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        instantiateDataCipher()
    }

    override fun getCipher(): DataCipher {
        return dataCipher
    }

    private fun instantiateDataCipher(): DataCipher {
        return if (isAndroidKeyStoreCipherAllowed()) {
            DataCipherImpl(KeyStoreSecretKeyProvider(), CipherTransformation.AES_CBC_PKCS7)
        } else {
            DataCipherImpl(FileSecretKeyProvider(context), CipherTransformation.AES_CBC_PKCS5)
        }
    }
}