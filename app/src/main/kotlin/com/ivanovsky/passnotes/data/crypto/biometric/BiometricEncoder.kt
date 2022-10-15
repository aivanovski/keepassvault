package com.ivanovsky.passnotes.data.crypto.biometric

import com.ivanovsky.passnotes.data.crypto.entity.SecretData

interface BiometricEncoder : BiometricDataCipher {
    fun encode(data: String): SecretData?
}