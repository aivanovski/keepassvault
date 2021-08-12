package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus

class DetermineDatabaseStatusUseCase {

    fun determineStatus(
        fsOptions: FSOptions,
        operation: OperationResult<*>
    ): DatabaseStatus {
        return when {
            !fsOptions.isWriteEnabled -> DatabaseStatus.READ_ONLY
            operation.isDeferred -> DatabaseStatus.CACHED
            else -> DatabaseStatus.NORMAL
        }
    }
}