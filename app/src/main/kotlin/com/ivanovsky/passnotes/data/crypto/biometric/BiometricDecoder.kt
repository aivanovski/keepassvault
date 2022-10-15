package com.ivanovsky.passnotes.data.crypto.biometric

interface BiometricDecoder : BiometricDataCipher {
    fun decode(data: ByteArray): String?
}