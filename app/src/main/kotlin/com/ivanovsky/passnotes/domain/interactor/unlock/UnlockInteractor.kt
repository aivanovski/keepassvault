package com.ivanovsky.passnotes.domain.interactor.unlock

import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.entity.BiometricData
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_RECORD_IS_ALREADY_EXISTS
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_SYNCHRONIZATION_TAKES_TOO_LONG
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.keepass.FileKeepassKey
import com.ivanovsky.passnotes.data.repository.keepass.KeepassImplementation
import com.ivanovsky.passnotes.data.repository.keepass.PasswordKeepassKey
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.DecodePasswordWithBiometricUseCase
import com.ivanovsky.passnotes.domain.usecases.FindNoteForAutofillUseCase
import com.ivanovsky.passnotes.domain.usecases.GetRecentlyOpenedFilesUseCase
import com.ivanovsky.passnotes.domain.usecases.GetUsedFileUseCase
import com.ivanovsky.passnotes.domain.usecases.RemoveBiometricDataUseCase
import com.ivanovsky.passnotes.domain.usecases.RemoveUsedFileUseCase
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases
import com.ivanovsky.passnotes.domain.usecases.test.GetTestPasswordUseCase
import com.ivanovsky.passnotes.extensions.isSyncInProgress
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class UnlockInteractor(
    private val fileRepository: UsedFileRepository,
    private val dbRepo: EncryptedDatabaseRepository,
    private val fileSystemResolver: FileSystemResolver,
    private val getFilesUseCase: GetRecentlyOpenedFilesUseCase,
    private val removeFileUseCase: RemoveUsedFileUseCase,
    private val autofillUseCase: FindNoteForAutofillUseCase,
    private val getTestPasswordUseCase: GetTestPasswordUseCase,
    private val syncUseCases: SyncUseCases,
    private val getUsedFileUseCase: GetUsedFileUseCase,
    private val decodePasswordUseCase: DecodePasswordWithBiometricUseCase,
    private val removeBiometricDataUseCase: RemoveBiometricDataUseCase,
    private val dispatchers: DispatcherProvider,
    private val settings: Settings
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

    suspend fun getRecentlyOpenedFiles(): OperationResult<List<UsedFile>> =
        getFilesUseCase.getRecentlyOpenedFiles()

    suspend fun removeFromUsedFiles(file: FileDescriptor): OperationResult<Boolean> =
        removeFileUseCase.removeUsedFile(file.uid, file.fsAuthority)

    suspend fun getSyncState(file: FileDescriptor): SyncState =
        syncUseCases.getSyncState(file)

    suspend fun resolveConflict(
        file: FileDescriptor,
        resolutionStrategy: ConflictResolutionStrategy
    ): OperationResult<FileDescriptor> =
        syncUseCases.resolveConflict(file, resolutionStrategy)

    suspend fun openDatabase(
        key: EncryptedDatabaseKey,
        file: FileDescriptor
    ): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            val awaitResult = awaitIfSyncInProgress(file)
            if (awaitResult.isFailed) {
                return@withContext awaitResult.mapError()
            }

            val result = if (canBeOpenedFromCache(file)) {
                dbRepo.open(
                    KeepassImplementation.KOTPASS,
                    key,
                    file,
                    FSOptions.CACHE_ONLY
                )
            } else {
                val fsOptions = FSOptions.DEFAULT.copy(
                    isPostponedSyncEnabled = settings.isPostponedSyncEnabled
                )

                dbRepo.open(
                    KeepassImplementation.KOTPASS,
                    key,
                    file,
                    fsOptions
                )
            }

            if (result.isSucceededOrDeferred) {
                updateUsedFile(file, key)
            }

            result.takeStatusWith(true)
        }

    private suspend fun awaitIfSyncInProgress(
        file: FileDescriptor
    ): OperationResult<Unit> =
        withContext(dispatchers.IO) {
            var syncProgress: SyncProgressStatus? = null
            var totalAwaitTime = 0L

            while (syncProgress == null || syncProgress.isSyncInProgress()) {
                syncProgress = syncUseCases.getSyncProgressStatus(file)

                if (syncProgress.isSyncInProgress()) {
                    delay(SYNC_AWAIT_STEP)
                    totalAwaitTime += SYNC_AWAIT_STEP

                    if (totalAwaitTime >= SYNC_AWAIT_TIMEOUT) {
                        return@withContext OperationResult.error(
                            newGenericError(MESSAGE_SYNCHRONIZATION_TAKES_TOO_LONG)
                        )
                    }
                }
            }

            OperationResult.success(Unit)
        }

    private fun canBeOpenedFromCache(file: FileDescriptor): Boolean {
        val fsType = file.fsAuthority.type
        if (fsType == FSType.EXTERNAL_STORAGE ||
            fsType == FSType.INTERNAL_STORAGE ||
            fsType == FSType.SAF
        ) {
            return false
        }

        val fsProvider = fileSystemResolver.resolveProvider(file.fsAuthority)
        val cachedFile = fsProvider.syncProcessor.getCachedFile(file.uid)
        return cachedFile != null
    }

    private fun shouldOpenDbFromCache(openResult: OperationResult<EncryptedDatabase>): Boolean {
        if (openResult.isSucceededOrDeferred) {
            return false
        }

        val error = openResult.error
        return error.type == OperationError.Type.DB_VERSION_CONFLICT_ERROR ||
            error.type == OperationError.Type.AUTH_ERROR
    }

    suspend fun findNoteForAutofill(
        structure: AutofillStructure
    ): OperationResult<Note?> =
        autofillUseCase.findNoteForAutofill(structure)

    private fun updateUsedFile(file: FileDescriptor, key: EncryptedDatabaseKey) {
        val usedFile = fileRepository.findByUid(file.uid, file.fsAuthority)
        if (usedFile != null) {
            val updatedFile = when (key) {
                is PasswordKeepassKey -> {
                    usedFile.copy(
                        lastAccessTime = System.currentTimeMillis(),
                        keyType = key.type,
                        keyFileFsAuthority = null,
                        keyFilePath = null,
                        keyFileUid = null,
                        keyFileName = null
                    )
                }

                is FileKeepassKey -> {
                    usedFile.copy(
                        lastAccessTime = System.currentTimeMillis(),
                        keyType = key.type,
                        keyFileFsAuthority = key.file.fsAuthority,
                        keyFilePath = key.file.path,
                        keyFileUid = key.file.uid,
                        keyFileName = key.file.name
                    )
                }

                else -> throw IllegalStateException()
            }

            fileRepository.update(updatedFile)
        }
    }

    suspend fun updateUsedFileFsAuthority(
        fileUid: String,
        oldFsAuthority: FSAuthority,
        newFsAuthority: FSAuthority
    ): OperationResult<UsedFile> =
        withContext(dispatchers.IO) {
            val getFileResult = getUsedFileUseCase.getUsedFile(fileUid, oldFsAuthority)
            if (getFileResult.isFailed) {
                return@withContext getFileResult.takeError()
            }

            val file = getFileResult.obj
            val updatedFile = file.copy(
                fsAuthority = newFsAuthority
            )

            fileRepository.update(updatedFile)

            OperationResult.success(updatedFile)
        }

    suspend fun removeKeyFile(file: UsedFile): OperationResult<Unit> =
        withContext(dispatchers.IO) {
            val existingFile = fileRepository.findByUid(file.fileUid, file.fsAuthority)
                ?: return@withContext OperationResult.error(
                    newDbError(
                        String.format(
                            GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID,
                            UsedFile::class.simpleName,
                            file.fileUid
                        )
                    )
                )

            val updatedFile = existingFile.copy(
                keyType = KeyType.PASSWORD,
                keyFileFsAuthority = null,
                keyFilePath = null,
                keyFileUid = null,
                keyFileName = null
            )

            fileRepository.update(updatedFile)

            OperationResult.success(Unit)
        }

    suspend fun saveUsedFileWithoutAccessTime(file: UsedFile): OperationResult<UsedFile> =
        withContext(dispatchers.IO) {
            val existingFile = fileRepository.findByUid(file.fileUid, file.fsAuthority)
            if (existingFile != null) {
                return@withContext OperationResult.error(
                    newDbError(MESSAGE_RECORD_IS_ALREADY_EXISTS)
                )
            }

            fileRepository.insert(file.copy(lastAccessTime = null))

            val insertedFile = fileRepository.findByUid(file.fileUid, file.fsAuthority)
                ?: return@withContext OperationResult.error(
                    newDbError(
                        String.format(
                            GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID,
                            UsedFile::class.simpleName,
                            file.fileUid
                        )
                    )
                )

            OperationResult.success(insertedFile)
        }

    suspend fun decodePassword(
        decoder: BiometricDecoder,
        biometricData: BiometricData
    ): OperationResult<String> =
        decodePasswordUseCase.decodePassword(decoder, biometricData)

    suspend fun removeBiometricData(
        usedFileId: Int
    ): OperationResult<Unit> =
        removeBiometricDataUseCase.removeBiometricData(usedFileId)

    suspend fun getUsedFile(
        usedFileId: Int
    ): OperationResult<UsedFile> =
        getUsedFileUseCase.getUsedFile(usedFileId)

    suspend fun getFile(
        path: String,
        fsAuthority: FSAuthority
    ): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            fileSystemResolver
                .resolveProvider(fsAuthority)
                .getFile(path, FSOptions.READ_ONLY)
        }

    suspend fun getTestPasswordForFile(
        filename: String
    ): String? =
        getTestPasswordUseCase.getTestPasswordForFile(filename)

    companion object {
        private val SYNC_AWAIT_TIMEOUT = TimeUnit.SECONDS.toMillis(15L)
        private val SYNC_AWAIT_STEP = TimeUnit.MILLISECONDS.toMillis(200L)
    }
}