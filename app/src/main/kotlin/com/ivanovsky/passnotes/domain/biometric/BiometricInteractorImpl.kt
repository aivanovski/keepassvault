package com.ivanovsky.passnotes.domain.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricCipherProvider
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
import com.ivanovsky.passnotes.data.crypto.entity.BiometricData

class BiometricInteractorImpl(
    context: Context
) : BiometricInteractor {

    private val biometricManager = BiometricManager.from(context)
    private val cipherProvider = BiometricCipherProvider()

    /**
     * Returns true if device support biometric authentication
     */
    override fun isBiometricUnlockAvailable(): Boolean {
        val type = BIOMETRIC_STRONG
        return biometricManager.canAuthenticate(type) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override fun getCipherForEncryption(): BiometricEncoder {
        return cipherProvider.getCipherForEncryption()
    }

    override fun getCipherForDecryption(biometricData: BiometricData): BiometricDecoder {
        return cipherProvider.getCipherForDecryption(
            initVector = biometricData.initVector
        )
    }
}