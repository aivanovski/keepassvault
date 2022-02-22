package com.ivanovsky.passnotes.domain.interactor.unlock

import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_RECORD_IS_ALREADY_EXISTS
import com.ivanovsky.passnotes.data.entity.OperationError.Type.NETWORK_IO_ERROR
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.FindNoteForAutofillUseCase
import com.ivanovsky.passnotes.domain.usecases.GetRecentlyOpenedFilesUseCase
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import kotlinx.coroutines.withContext

class UnlockInteractor(
    private val fileRepository: UsedFileRepository,
    private val dbRepo: EncryptedDatabaseRepository,
    private val dispatchers: DispatcherProvider,
    private val getFilesUseCase: GetRecentlyOpenedFilesUseCase,
    private val autofillUseCase: FindNoteForAutofillUseCase,
    private val syncUseCases: SyncUseCases
) {

    fun hasActiveDatabase(): Boolean {
        return dbRepo.isOpened
    }

    fun closeActiveDatabase(): OperationResult<Unit> {
        if (!dbRepo.isOpened) {
            return OperationResult.success(Unit)
        }

        val closeResult = dbRepo.close()
        return closeResult.takeStatusWith(Unit)
    }

    suspend fun getRecentlyOpenedFiles(): OperationResult<List<FileDescriptor>> =
        getFilesUseCase.getRecentlyOpenedFiles()

    suspend fun getSyncConflictInfo(file: FileDescriptor): OperationResult<SyncConflictInfo> =
        syncUseCases.getSyncConflictInfo(file)

    suspend fun getSyncStatus(file: FileDescriptor): SyncStatus =
        syncUseCases.getSyncStatus(file)

    suspend fun resolveConflict(
        file: FileDescriptor,
        resolutionStrategy: ConflictResolutionStrategy
    ): OperationResult<FileDescriptor> =
        syncUseCases.resolveConflict(file, resolutionStrategy)

    suspend fun openDatabase(
        key: KeepassDatabaseKey,
        file: FileDescriptor
    ): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            val syncStatus = syncUseCases.getSyncStatus(file)

            if (syncStatus == SyncStatus.LOCAL_CHANGES || syncStatus == SyncStatus.REMOTE_CHANGES) {
                val syncResult = syncUseCases.processSync(file)
                if (syncResult.isFailed && syncResult.error.type != NETWORK_IO_ERROR) {
                    return@withContext syncResult.takeError()
                }
            }

            val open = dbRepo.open(key, file, FSOptions.DEFAULT)

            val result = if (open.isFailed &&
                open.error.type == OperationError.Type.DB_VERSION_CONFLICT_ERROR) {
                val cachedDb = dbRepo.open(key, file, FSOptions.CACHE_ONLY)
                if (cachedDb.isSucceededOrDeferred) {
                    cachedDb
                } else {
                    open
                }
            } else {
                open
            }

            if (result.isSucceededOrDeferred) {
                updateFileAccessTime(file)
            }

            result.takeStatusWith(true)
        }

    suspend fun findNoteForAutofill(
        structure: AutofillStructure
    ): OperationResult<Pair<Boolean, Note?>> =
        autofillUseCase.findNoteForAutofill(structure)

    private fun updateFileAccessTime(file: FileDescriptor) {
        val usedFile = fileRepository.findByUid(file.uid, file.fsAuthority)
        if (usedFile != null) {
            val updatedFile = usedFile.copy(
                lastAccessTime = System.currentTimeMillis()
            )

            fileRepository.update(updatedFile)
        }
    }

    fun saveUsedFileWithoutAccessTime(file: UsedFile): OperationResult<Boolean> {
        val result = OperationResult<Boolean>()

        val existing = fileRepository.findByUid(file.fileUid, file.fsAuthority)
        if (existing == null) {
            val newFile = file.copy(
                lastAccessTime = null
            )

            fileRepository.insert(newFile)

            result.obj = true
        } else {
            result.obj = false
            result.error = newDbError(MESSAGE_RECORD_IS_ALREADY_EXISTS)
        }

        return result
    }

    companion object {
        private val TAG = UnlockInteractor::class.simpleName
    }
}
