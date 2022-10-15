package com.ivanovsky.passnotes.data.crypto.biometric

import javax.crypto.Cipher

interface BiometricDataCipher {
    fun getCipher(): Cipher
}