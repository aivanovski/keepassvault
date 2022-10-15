package com.ivanovsky.passnotes.data.crypto.biometric

import javax.crypto.Cipher

class BiometricDecoderImpl(
    private val cipher: Cipher
) : BiometricDecoder {

    override fun getCipher(): Cipher = cipher

    override fun decode(data: ByteArray): String? {
        val result = cipher.doFinal(data)
        if (result == null || result.isEmpty()) {
            return null
        }

        return String(result)
    }
}