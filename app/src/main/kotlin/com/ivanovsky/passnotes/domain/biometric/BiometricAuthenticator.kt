package com.ivanovsky.passnotes.domain.biometric

import androidx.fragment.app.FragmentActivity
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDataCipher
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder

interface BiometricAuthenticator {

    fun authenticateForUnlock(
        activity: FragmentActivity,
        cipher: BiometricDataCipher,
        onSuccess: (decoder: BiometricDecoder) -> Unit,
    )

    fun authenticateForSetup(
        activity: FragmentActivity,
        cipher: BiometricDataCipher,
        onSuccess: (encoder: BiometricEncoder) -> Unit,
    )
}