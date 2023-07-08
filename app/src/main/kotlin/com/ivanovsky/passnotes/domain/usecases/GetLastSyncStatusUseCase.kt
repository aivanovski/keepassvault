package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.keepass.DatabaseSyncStatusProvider
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext

class GetLastSyncStatusUseCase(
    private val syncStatusProvider: DatabaseSyncStatusProvider,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getLastSyncStatus(): OperationResult<SyncStatus?> {
        return withContext(dispatchers.IO) {
            val status = syncStatusProvider.status
            OperationResult.success(status)
        }
    }
}