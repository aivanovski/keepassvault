package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.extensions.mapError
import kotlinx.coroutines.withContext

class RemoveBiometricDataUseCase(
    private val dispatchers: DispatcherProvider,
    private val usedFileRepository: UsedFileRepository,
    private val getUsedFileUseCase: GetUsedFileUseCase
) {

    suspend fun removeBiometricData(usedFileId: Int): OperationResult<Unit> =
        withContext(dispatchers.IO) {
            val getFileResult = getUsedFileUseCase.getUsedFile(usedFileId)
            if (getFileResult.isFailed) {
                return@withContext getFileResult.mapError()
            }

            val updatedFile = getFileResult.obj.copy(
                biometricData = null
            )

            usedFileRepository.update(updatedFile)

            OperationResult.success(Unit)
        }

    suspend fun removeAllBiometricData() =
        withContext(dispatchers.IO) {
            val files = usedFileRepository.getAll()
                .filter { it.biometricData != null }

            for (file in files) {
                usedFileRepository.update(
                    file.copy(
                        biometricData = null
                    )
                )
            }
        }
}