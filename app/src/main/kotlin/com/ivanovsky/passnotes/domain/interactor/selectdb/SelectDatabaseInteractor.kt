package com.ivanovsky.passnotes.domain.interactor.selectdb

import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.GetRecentlyOpenedFilesUseCase
import com.ivanovsky.passnotes.domain.usecases.RemoveUsedFileUseCase
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases
import com.ivanovsky.passnotes.extensions.getFileDescriptor
import kotlinx.coroutines.withContext

class SelectDatabaseInteractor(
    private val getFilesUseCase: GetRecentlyOpenedFilesUseCase,
    private val removeFileUseCase: RemoveUsedFileUseCase,
    private val syncUseCases: SyncUseCases,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getRecentlyOpenedFiles(): OperationResult<List<FileDescriptor>> =
        withContext(dispatchers.IO) {
            val getFilesResult = getFilesUseCase.getRecentlyOpenedFiles()
            if (getFilesResult.isFailed) {
                return@withContext getFilesResult.takeError()
            }

            val files = getFilesResult.obj
                .map { it.getFileDescriptor() }

            OperationResult.success(files)
        }

    suspend fun removeFromUsedFiles(file: FileDescriptor): OperationResult<Boolean> =
        removeFileUseCase.removeUsedFile(file.uid, file.fsAuthority)

    suspend fun getSyncConflictInfo(file: FileDescriptor): OperationResult<SyncConflictInfo> =
        syncUseCases.getSyncConflictInfo(file)

    suspend fun getSyncState(file: FileDescriptor): SyncState =
        syncUseCases.getSyncState(file)

    suspend fun resolveConflict(
        file: FileDescriptor,
        resolutionStrategy: ConflictResolutionStrategy
    ): OperationResult<FileDescriptor> =
        syncUseCases.resolveConflict(file, resolutionStrategy)
}