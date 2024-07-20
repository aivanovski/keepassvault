package com.ivanovsky.passnotes.domain.test.biometric

import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
import com.ivanovsky.passnotes.data.crypto.entity.BiometricData
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.biometric.BiometricInteractor
import java.util.concurrent.atomic.AtomicBoolean

class FakeBiometricInteractorImpl : BiometricInteractor {

    private val encoder = ClearTextBiometricEncoder()
    private val decoder = ClearTextBiometricDecoder()

    private val isBiometricDataInvalidated = AtomicBoolean(false)

    override fun isBiometricUnlockAvailable(): Boolean = true

    override fun getCipherForEncryption(): OperationResult<BiometricEncoder> {
        return if (isBiometricDataInvalidated.get()) {
            OperationResult.error(newBiometricDataInvalidatedError())
        } else {
            OperationResult.success(encoder)
        }
    }

    override fun getCipherForDecryption(
        biometricData: BiometricData
    ): OperationResult<BiometricDecoder> {
        return if (isBiometricDataInvalidated.get()) {
            OperationResult.error(newBiometricDataInvalidatedError())
        } else {
            OperationResult.success(decoder)
        }
    }

    override fun clearStoredData(): OperationResult<Boolean> {
        val result = isBiometricDataInvalidated.compareAndSet(true, false)
        return OperationResult.success(result)
    }

    fun triggerBiometricDataInvalidated() {
        isBiometricDataInvalidated.set(true)
    }

    private fun newBiometricDataInvalidatedError(): OperationError {
        return OperationError(OperationError.Type.BIOMETRIC_DATA_INVALIDATED_ERROR)
    }
}