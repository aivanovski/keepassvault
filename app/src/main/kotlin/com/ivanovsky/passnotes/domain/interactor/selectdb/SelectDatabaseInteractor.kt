package com.ivanovsky.passnotes.domain.interactor.selectdb

import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.GetRecentlyOpenedFilesUseCase
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases
import kotlinx.coroutines.withContext

class SelectDatabaseInteractor(
    private val dispatchers: DispatcherProvider,
    private val getFilesUseCase: GetRecentlyOpenedFilesUseCase,
    private val fileRepository: UsedFileRepository,
    private val syncUseCases: SyncUseCases
) {

    suspend fun getRecentlyOpenedFiles(): OperationResult<List<FileDescriptor>> =
        getFilesUseCase.getRecentlyOpenedFiles()

    suspend fun removeFromUsedFiles(file: FileDescriptor): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            val usedFile = fileRepository.getAll()
                .firstOrNull {
                    it.fsAuthority == file.fsAuthority &&
                        it.filePath == file.path &&
                        it.fileUid == file.uid
                }

            if (usedFile != null) {
                usedFile.id?.let { id ->
                    fileRepository.remove(id)
                }
                OperationResult.success(true)
            } else {
                OperationResult.success(false)
            }
        }

    suspend fun getSyncConflictInfo(file: FileDescriptor): OperationResult<SyncConflictInfo> =
        syncUseCases.getSyncConflictInfo(file)

    suspend fun getSyncStatus(file: FileDescriptor): SyncStatus =
        syncUseCases.getSyncStatus(file)

    suspend fun resolveConflict(
        file: FileDescriptor,
        resolutionStrategy: ConflictResolutionStrategy
    ): OperationResult<FileDescriptor> =
        syncUseCases.resolveConflict(file, resolutionStrategy)
}