package com.ivanovsky.passnotes.domain.biometric

import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
import com.ivanovsky.passnotes.data.crypto.entity.BiometricData

class FakeBiometricInteractorImpl : BiometricInteractor {

    private val encoder = ClearTextBiometricEncoder()
    private val decoder = ClearTextBiometricDecoder()

    override fun isBiometricUnlockAvailable(): Boolean = true

    override fun getCipherForEncryption(): BiometricEncoder = encoder

    override fun getCipherForDecryption(biometricData: BiometricData): BiometricDecoder = decoder
}