package com.ivanovsky.passnotes.domain.interactor.selectdb

import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.domain.usecases.GetRecentlyOpenedFilesUseCase
import com.ivanovsky.passnotes.domain.usecases.RemoveUsedFileUseCase
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases

class SelectDatabaseInteractor(
    private val getFilesUseCase: GetRecentlyOpenedFilesUseCase,
    private val removeFileUseCase: RemoveUsedFileUseCase,
    private val syncUseCases: SyncUseCases
) {

    suspend fun getRecentlyOpenedFiles(): OperationResult<List<FileDescriptor>> =
        getFilesUseCase.getRecentlyOpenedFiles()

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