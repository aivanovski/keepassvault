package com.ivanovsky.passnotes.domain.biometric

import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import javax.crypto.Cipher

class ClearTextBiometricDecoder : BiometricDecoder {
    override fun getCipher(): Cipher {
        throw IllegalStateException()
    }

    override fun decode(data: ByteArray): String {
        return String(data)
    }
}