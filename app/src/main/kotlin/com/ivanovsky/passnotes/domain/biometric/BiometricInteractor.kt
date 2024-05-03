package com.ivanovsky.passnotes.domain.biometric

import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
import com.ivanovsky.passnotes.data.crypto.entity.BiometricData
import com.ivanovsky.passnotes.data.entity.OperationResult

interface BiometricInteractor {
    /**
     * Returns true if device support biometric authentication
     */
    fun isBiometricUnlockAvailable(): Boolean
    fun getCipherForEncryption(): OperationResult<BiometricEncoder>
    fun getCipherForDecryption(biometricData: BiometricData): OperationResult<BiometricDecoder>
    fun clearStoredData(): OperationResult<Boolean>
}