package com.ivanovsky.passnotes.domain.biometric

import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
import com.ivanovsky.passnotes.data.crypto.entity.SecretData
import javax.crypto.Cipher

class ClearTextBiometricEncoder : BiometricEncoder {
    override fun getCipher(): Cipher {
        throw IllegalStateException()
    }

    override fun encode(data: String): SecretData {
        return SecretData(
            initVector = INIT_VECTOR,
            encryptedData = data.toByteArray()
        )
    }

    companion object {
        private val INIT_VECTOR = "initVector".toByteArray()
    }
}