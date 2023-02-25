package com.ivanovsky.passnotes.data.crypto.keyprovider

import android.content.Context
import com.ivanovsky.passnotes.data.crypto.DataCipherConstants
import com.ivanovsky.passnotes.data.crypto.DataCipherConstants.KEY_ALIAS
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import timber.log.Timber

class FileSecretKeyProvider(
    private val context: Context
) : SecretKeyProvider {

    private var keyStore: KeyStore? = null

    override fun getSecretKey(isCreateIfNeed: Boolean): SecretKey? {
        var key: SecretKey?

        val keyStore = getKeyStore(isCreateIfNeed)
            ?: return null

        key = obtainSecretKeyFromKeyStore(keyStore)
        if (key == null && isCreateIfNeed) {
            val generatedKey = generateNewSecretKey()
            if (generatedKey != null && saveKey(generatedKey)) {
                key = generatedKey
            }
        }

        return key
    }

    private fun getKeyStore(isCreateIfNeed: Boolean): KeyStore? {
        if (keyStore != null) return keyStore

        val keyStoreFile = getKeyStoreFile()

        try {
            val store = KeyStore.getInstance(KeyStore.getDefaultType())

            keyStore = when {
                keyStoreFile.exists() -> {
                    store?.load(FileInputStream(keyStoreFile), KEY_STORE_PASSWORD.toCharArray())
                    store
                }
                isCreateIfNeed -> {
                    store?.load(null)
                    store
                }
                else -> {
                    null
                }
            }
        } catch (e: Exception) {
            Timber.d(e)
        }

        return keyStore
    }

    private fun saveKey(secretKey: SecretKey): Boolean {
        var result = false

        val keyStore = this.keyStore
        if (keyStore != null) {
            try {
                keyStore.setEntry(
                    KEY_ALIAS,
                    KeyStore.SecretKeyEntry(secretKey),
                    KeyStore.PasswordProtection(KEY_STORE_PASSWORD.toCharArray())
                )
                keyStore.store(
                    FileOutputStream(getKeyStoreFile()),
                    KEY_STORE_PASSWORD.toCharArray()
                )
                result = true
            } catch (e: Exception) {
                Timber.d(e)
            }
        }

        Timber.d("Saving key store, success: $result")

        return result
    }

    private fun obtainSecretKeyFromKeyStore(keyStore: KeyStore): SecretKey? {
        var key: SecretKey? = null

        try {
            val entry = keyStore.getEntry(
                KEY_ALIAS,
                KeyStore.PasswordProtection(KEY_STORE_PASSWORD.toCharArray())
            )
            if (entry is KeyStore.SecretKeyEntry) {
                key = entry.secretKey
            }
        } catch (e: Exception) {
            Timber.d(e)
        }

        Timber.d("Load secret key from key store with algorithm: " + key?.algorithm)

        return key
    }

    private fun generateNewSecretKey(): SecretKey? {
        var key: SecretKey? = null

        try {
            val keyGenerator = KeyGenerator.getInstance(DataCipherConstants.KEY_ALGORITHM)
            keyGenerator?.init(DataCipherConstants.KEY_SIZE)
            key = keyGenerator?.generateKey()
        } catch (e: NoSuchAlgorithmException) {
            Timber.d(e)
        }

        Timber.d(TAG, "Generate new secret key with algorithm: " + key?.algorithm)

        return key
    }

    private fun getKeyStoreFile(): File {
        return File(context.filesDir, KEY_STORE_FILE_NAME)
    }

    companion object {
        private val TAG = FileSecretKeyProvider::class.simpleName
        const val KEY_STORE_FILE_NAME = "Passnotes"
        const val KEY_STORE_PASSWORD = "setonssaP"
    }
}