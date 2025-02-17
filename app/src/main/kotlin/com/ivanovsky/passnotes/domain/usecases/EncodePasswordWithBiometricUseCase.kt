package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
import com.ivanovsky.passnotes.data.crypto.entity.BiometricData
import com.ivanovsky.passnotes.data.crypto.entity.toBiometricData
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_ENCODE_DATA
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import kotlinx.coroutines.withContext

class EncodePasswordWithBiometricUseCase(private val dispatchers: DispatcherProvider) {

    suspend fun encodePassword(
        encoder: BiometricEncoder,
        password: String
    ): OperationResult<BiometricData> =
        withContext(dispatchers.IO) {
            val encryptedData = encoder.encode(password)
                ?: return@withContext OperationResult.error(
                    newGenericError(
                        MESSAGE_FAILED_TO_ENCODE_DATA,
                        Stacktrace()
                    )
                )

            OperationResult.success(encryptedData.toBiometricData())
        }
}