package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext

class RemoveUsedFileUseCase(
    private val fileRepository: UsedFileRepository,
    private val dispatchers: DispatcherProvider
) {

    suspend fun removeUsedFile(uid: String, fsAuthority: FSAuthority): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            val usedFile = fileRepository.findByUid(uid, fsAuthority)
                ?: return@withContext OperationResult.error(
                    newDbError(
                        String.format(
                            GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID,
                            UsedFile::class.simpleName,
                            uid
                        )
                    )
                )

            usedFile.id?.let {
                fileRepository.remove(it)
            }

            OperationResult.success(true)
        }
}