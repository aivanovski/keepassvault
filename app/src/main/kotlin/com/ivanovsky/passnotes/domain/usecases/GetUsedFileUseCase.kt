package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext

class GetUsedFileUseCase(
    private val fileRepository: UsedFileRepository,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getUsedFile(
        fileUid: String,
        fsAuthority: FSAuthority
    ): OperationResult<UsedFile> =
        withContext(dispatchers.IO) {
            val file = fileRepository.findByUid(fileUid, fsAuthority)
            if (file != null) {
                OperationResult.success(file)
            } else {
                OperationResult.error(
                    newDbError(
                        String.format(
                            OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID,
                            UsedFile::class.simpleName,
                            fileUid
                        )
                    )
                )
            }
        }
}