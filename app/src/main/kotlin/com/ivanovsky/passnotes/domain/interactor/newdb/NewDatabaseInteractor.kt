package com.ivanovsky.passnotes.domain.interactor.newdb

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError.*
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.AddTemplatesUseCase
import com.ivanovsky.passnotes.extensions.toUsedFile
import kotlinx.coroutines.withContext

class NewDatabaseInteractor(
    private val dbRepo: EncryptedDatabaseRepository,
    private val usedFileRepository: UsedFileRepository,
    private val fileSystemResolver: FileSystemResolver,
    private val addTemplatesUseCase: AddTemplatesUseCase,
    private val dispatchers: DispatcherProvider
) {

    suspend fun createNewDatabaseAndOpen(
        key: KeepassDatabaseKey,
        file: FileDescriptor,
        isAddTemplates: Boolean
    ): OperationResult<Boolean> {
        return withContext(dispatchers.IO) {
            val provider = fileSystemResolver.resolveProvider(file.fsAuthority)

            val existsResult = provider.exists(file)
            if (existsResult.isFailed) {
                return@withContext existsResult.takeError()
            }

            val isExists = existsResult.obj
            if (isExists) {
                return@withContext OperationResult.error(newFileIsAlreadyExistsError())
            }

            val creationResult = dbRepo.createNew(key, file, isAddTemplates)
            if (creationResult.isFailed) {
                return@withContext creationResult.takeError()
            } else if (creationResult.isDeferred) {
                return@withContext OperationResult.error(
                    newFileAccessError(MESSAGE_DEFERRED_OPERATIONS_ARE_NOT_SUPPORTED)
                )
            }

            val openResult = dbRepo.open(key, file, FSOptions.DEFAULT)
            if (openResult.isFailed) {
                return@withContext openResult.takeError()
            }

            val getFileResult = getFile(file)
            if (getFileResult.isFailed) {
                return@withContext getFileResult.takeError()
            }

            val newFile = getFileResult.obj
            val time = System.currentTimeMillis()
            val usedFile = newFile.toUsedFile(
                addedTime = time,
                lastAccessTime = time
            )

            usedFileRepository.insert(usedFile)

            OperationResult.success(true)
        }
    }

    private fun getFile(file: FileDescriptor): OperationResult<FileDescriptor> {
        val provider = fileSystemResolver.resolveProvider(file.fsAuthority)
        val getFileResult = provider.getFile(file.path, FSOptions.DEFAULT)

        return when {
            getFileResult.isSucceeded -> getFileResult
            getFileResult.isDeferred -> OperationResult.error(
                newFileAccessError(MESSAGE_DEFERRED_OPERATIONS_ARE_NOT_SUPPORTED)
            )
            else -> getFileResult.takeError()
        }
    }
}