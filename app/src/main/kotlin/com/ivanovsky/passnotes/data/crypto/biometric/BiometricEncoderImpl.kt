package com.ivanovsky.passnotes.data.crypto.biometric

import com.ivanovsky.passnotes.data.crypto.entity.SecretData
import javax.crypto.Cipher

class BiometricEncoderImpl(
    private val cipher: Cipher,
) : BiometricEncoder {

    override fun getCipher(): Cipher = cipher

    override fun encode(data: String): SecretData? {
        val encryptedData = cipher.doFinal(data.toByteArray())
        if (encryptedData == null || encryptedData.isEmpty()) {
            return null
        }

        return SecretData(
            initVector = cipher.iv,
            encryptedData = encryptedData
        )
    }
}