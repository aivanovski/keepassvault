package com.ivanovsky.passnotes.domain.biometric

import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
import com.ivanovsky.passnotes.data.crypto.entity.BiometricData

interface BiometricInteractor {
    /**
     * Returns true if device support biometric authentication
     */
    fun isBiometricUnlockAvailable(): Boolean
    fun getCipherForEncryption(): BiometricEncoder
    fun getCipherForDecryption(biometricData: BiometricData): BiometricDecoder
}