package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.entity.BiometricData
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_DECODE_DATA
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import kotlinx.coroutines.withContext

class DecodePasswordWithBiometricUseCase(
    private val dispatchers: DispatcherProvider
) {

    suspend fun decodePassword(
        decoder: BiometricDecoder,
        biometricData: BiometricData
    ): OperationResult<String> =
        withContext(dispatchers.IO) {
            val password = decoder.decode(biometricData.encryptedData)
            if (password.isNullOrEmpty()) {
                return@withContext OperationResult.error(
                    OperationError.newGenericError(
                        MESSAGE_FAILED_TO_DECODE_DATA,
                        Stacktrace()
                    )
                )
            }

            OperationResult.success(password)
        }
}