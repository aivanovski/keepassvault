package com.ivanovsky.passnotes.domain.test.biometric

import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
import com.ivanovsky.passnotes.data.crypto.entity.BiometricData
import com.ivanovsky.passnotes.data.entity.OperationError.newBiometricDataError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.biometric.BiometricAuthenticator
import com.ivanovsky.passnotes.domain.biometric.BiometricInteractor
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import java.util.concurrent.atomic.AtomicBoolean

class DebugBiometricInteractorImpl(
    resourceProvider: ResourceProvider
) : BiometricInteractor {

    private val encoder = ClearTextBiometricEncoder()
    private val decoder = ClearTextBiometricDecoder()
    private val authenticator = DebugBiometricAuthenticatorImpl(resourceProvider)
    private val isBiometricDataInvalidated = AtomicBoolean(false)

    override fun getAuthenticator(): BiometricAuthenticator = authenticator

    override fun isBiometricUnlockAvailable(): Boolean = true

    override fun getCipherForEncryption(): OperationResult<BiometricEncoder> {
        return if (isBiometricDataInvalidated.get()) {
            OperationResult.error(newBiometricDataError(Stacktrace()))
        } else {
            OperationResult.success(encoder)
        }
    }

    override fun getCipherForDecryption(
        biometricData: BiometricData
    ): OperationResult<BiometricDecoder> {
        return if (isBiometricDataInvalidated.get()) {
            OperationResult.error(newBiometricDataError(Stacktrace()))
        } else {
            OperationResult.success(decoder)
        }
    }

    override fun clearStoredData(): OperationResult<Boolean> {
        val result = isBiometricDataInvalidated.compareAndSet(true, false)
        return OperationResult.success(result)
    }
}