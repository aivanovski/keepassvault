package com.ivanovsky.passnotes.domain.interactor.unlock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricCipherProvider
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
import com.ivanovsky.passnotes.data.crypto.entity.BiometricData
import com.ivanovsky.passnotes.data.crypto.entity.toBiometricData
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_ID
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_DECODE_DATA
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_ENCODE_DATA
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_NO_DATA_TO_DECODE
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext

class BiometricUnlockInteractor(
    private val dispatchers: DispatcherProvider,
    private val usedFileRepository: UsedFileRepository,
    context: Context
) {

    private val biometricManager = BiometricManager.from(context)
    private val cipherProvider = BiometricCipherProvider()

    /**
     * Returns true if device support biometric authentication
     */
    fun isBiometricUnlockAvailable(): Boolean {
        val type = BIOMETRIC_STRONG
        return biometricManager.canAuthenticate(type) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun getCipherForEncryption(): BiometricEncoder {
        return cipherProvider.getCipherForEncryption()
    }

    fun getCipherForDecryption(biometricData: BiometricData): BiometricDecoder {
        return cipherProvider.getCipherForDecryption(
            initVector = biometricData.initVector
        )
    }

    suspend fun removeAllBiometricData() =
        withContext(dispatchers.IO) {
            val filesToRemove = usedFileRepository.getAll()
                .filter { it.biometricData != null }

            for (file in filesToRemove) {
                usedFileRepository.update(
                    file.copy(
                        biometricData = null
                    )
                )
            }
        }

    suspend fun decodePassword(
        decoder: BiometricDecoder,
        usedFile: UsedFile
    ): OperationResult<String> =
        withContext(dispatchers.IO) {
            val data = usedFile.biometricData
                ?: return@withContext OperationResult.error(
                    newGenericError(MESSAGE_NO_DATA_TO_DECODE)
                )

            val password = decoder.decode(data.encryptedData)
            if (password.isNullOrEmpty()) {
                return@withContext OperationResult.error(
                    newGenericError(MESSAGE_FAILED_TO_DECODE_DATA)
                )
            }

            OperationResult.success(password)
        }

    suspend fun encodeAndStorePasswordForFile(
        encoder: BiometricEncoder,
        password: String,
        usedFile: UsedFile
    ): OperationResult<Unit> =
        withContext(dispatchers.IO) {
            val encryptedData = encoder.encode(password)
                ?: return@withContext OperationResult.error(
                    newGenericError(MESSAGE_FAILED_TO_ENCODE_DATA)
                )

            val dbFile = usedFileRepository.findById(usedFile.id ?: -1)
                ?: return@withContext OperationResult.error(
                    newGenericError(
                        String.format(
                            GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_ID,
                            UsedFile::class.simpleName,
                            usedFile.id
                        )
                    )
                )

            usedFileRepository.update(
                dbFile.copy(
                    biometricData = encryptedData.toBiometricData()
                )
            )

            OperationResult.success(Unit)
        }
}