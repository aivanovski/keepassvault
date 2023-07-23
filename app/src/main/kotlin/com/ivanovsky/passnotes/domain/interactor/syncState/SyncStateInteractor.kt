package com.ivanovsky.passnotes.domain.interactor.syncState

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases

class SyncStateInteractor(
    val cache: SyncStateCache,
    private val syncUseCases: SyncUseCases,
    private val getDatabaseUseCase: GetDatabaseUseCase
) {

    suspend fun getSyncState(file: FileDescriptor): SyncState =
        syncUseCases.getSyncState(file)

    fun getDatabase(): OperationResult<EncryptedDatabase> =
        getDatabaseUseCase.getDatabaseSynchronously()

    suspend fun processSync(file: FileDescriptor): OperationResult<FileDescriptor> =
        syncUseCases.processSync(file)
}