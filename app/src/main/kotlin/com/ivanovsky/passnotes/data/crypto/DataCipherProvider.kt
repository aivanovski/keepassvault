package com.ivanovsky.passnotes.data.crypto

import android.content.Context
import com.ivanovsky.passnotes.data.crypto.entity.CipherTransformation
import com.ivanovsky.passnotes.data.crypto.keyprovider.FileSecretKeyProvider
import com.ivanovsky.passnotes.data.crypto.keyprovider.KeyStoreSecretKeyProvider

class DataCipherProvider(
    private val context: Context
) {

    val cipher: DataCipher by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { instantiateDataCipher() }

    private fun instantiateDataCipher(): DataCipher {
        return if (DataCipher.isAndroidKeyStoreCipherAllowed()) {
            DataCipher(KeyStoreSecretKeyProvider(), CipherTransformation.AES_CBC_PKCS7)
        } else {
            DataCipher(FileSecretKeyProvider(context), CipherTransformation.AES_CBC_PKCS5)
        }
    }
}