package com.ivanovsky.passnotes.data.crypto.biometric

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BiometricCipherProvider {

    fun getCipherForEncryption(): OperationResult<BiometricEncoder> {
        return try {
            val cipher = newCipher()
                .apply {
                    init(
                        Cipher.ENCRYPT_MODE,
                        getOrCreateKey()
                    )
                }

            OperationResult.success(BiometricEncoderImpl(cipher))
        } catch (exception: KeyPermanentlyInvalidatedException) {
            val error = OperationError(
                OperationError.Type.BIOMETRIC_DATA_INVALIDATED_ERROR,
                exception
            )

            OperationResult.error(error)
        }
    }

    fun getCipherForDecryption(initVector: ByteArray): OperationResult<BiometricDecoder> {
        return try {
            val cipher = newCipher()
                .apply {
                    init(
                        Cipher.DECRYPT_MODE,
                        getOrCreateKey(),
                        GCMParameterSpec(128, initVector)
                    )
                }

            OperationResult.success(BiometricDecoderImpl(cipher))
        } catch (exception: KeyPermanentlyInvalidatedException) {
            val error = OperationError(
                OperationError.Type.BIOMETRIC_DATA_INVALIDATED_ERROR,
                exception
            )

            OperationResult.error(error)
        }
    }

    fun clear(): OperationResult<Boolean> {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            .apply {
                load(null)
            }

        val isCleared = if (keyStore.getKey(KEY_NAME, null) != null) {
            keyStore.deleteEntry(KEY_NAME)
            true
        } else {
            false
        }

        return OperationResult.success(isCleared)
    }

    private fun newCipher(): Cipher {
        val transformation = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
        return Cipher.getInstance(transformation)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            .apply {
                load(null)
            }

        val key = keyStore.getKey(KEY_NAME, null)
        if (key != null) {
            return key as SecretKey
        }

        val paramsBuilder = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        paramsBuilder.apply {
            setBlockModes(ENCRYPTION_BLOCK_MODE)
            setEncryptionPaddings(ENCRYPTION_PADDING)
            setKeySize(KEY_SIZE)
            setUserAuthenticationRequired(true)
        }

        val keyGenParams = paramsBuilder.build()
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(keyGenParams)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val KEY_SIZE = 256
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val KEY_NAME = "biometric_secret_key"
    }
}