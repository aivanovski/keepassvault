package com.ivanovsky.passnotes.data.crypto.keyprovider

import android.annotation.TargetApi
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.ivanovsky.passnotes.data.crypto.DataCipherConstants
import com.ivanovsky.passnotes.data.crypto.DataCipherConstants.ANDROID_KEY_STORE
import com.ivanovsky.passnotes.data.crypto.DataCipherConstants.KEY_ALGORITHM
import com.ivanovsky.passnotes.data.crypto.DataCipherConstants.KEY_ALIAS
import com.ivanovsky.passnotes.data.crypto.DataCipherConstants.KEY_SIZE
import com.ivanovsky.passnotes.util.Logger
import java.lang.Exception
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.KeyStore.getInstance
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@TargetApi(23)
class KeyStoreSecretKeyProvider : SecretKeyProvider {

    private var keyStore: KeyStore? = null

    override fun getSecretKey(isCreateIfNeed: Boolean): SecretKey? {
        var key: SecretKey?

        val keyStore = getKeyStore() ?: return null

        key = obtainSecretKeyFromKeyStore(keyStore)
        if (key == null && isCreateIfNeed) {
            val generatedKey = generateNewSecretKey()
            if (generatedKey != null && refreshKeyStore()) {
                key = generatedKey
            }
        }

        return key
    }

    private fun getKeyStore(): KeyStore? {
        if (keyStore != null) return keyStore

        try {
            val store = getInstance(ANDROID_KEY_STORE)
            store?.load(null)
            keyStore = store
        } catch (e: Exception) {
            Logger.printStackTrace(e)
        }

        return keyStore
    }

    private fun refreshKeyStore(): Boolean {
        // just reload AndroidKeyStore because new key is automatically stored in it immediately
        // after it was generated
        keyStore = null
        return getKeyStore() != null
    }

    private fun obtainSecretKeyFromKeyStore(keyStore: KeyStore): SecretKey? {
        var key: SecretKey? = null

        try {
            val entry = keyStore.getEntry(DataCipherConstants.KEY_ALIAS, null)
            if (entry is KeyStore.SecretKeyEntry) {
                key = entry.secretKey
            }
        } catch (e: Exception) {
            Logger.printStackTrace(e)
        }

        Logger.d(TAG, "Load secret key from key store with algorithm: " + key?.algorithm)

        return key
    }

    private fun generateNewSecretKey(): SecretKey? {
        var key: SecretKey? = null

        try {
            val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM, ANDROID_KEY_STORE)

            val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            val keyParams = KeyGenParameterSpec.Builder(KEY_ALIAS, purposes)
                .setKeySize(KEY_SIZE)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build()

            keyGenerator?.init(keyParams)

            key = keyGenerator?.generateKey()
        } catch (e: GeneralSecurityException) {
            Logger.printStackTrace(e)
        }

        Logger.d(TAG, "Generate new secret key with algorithm: " + key?.algorithm)

        return key
    }

    companion object {
        private val TAG = KeyStoreSecretKeyProvider::class.simpleName
    }
}
