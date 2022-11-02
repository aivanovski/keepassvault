package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext

class UpdateUsedFileUseCase(
    private val usedFileRepository: UsedFileRepository,
    private val dispatchers: DispatcherProvider
) {

    suspend fun updateUsedFile(usedFile: UsedFile): OperationResult<Unit> =
        withContext(dispatchers.IO) {
            usedFileRepository.update(usedFile)

            OperationResult.success(Unit)
        }
}