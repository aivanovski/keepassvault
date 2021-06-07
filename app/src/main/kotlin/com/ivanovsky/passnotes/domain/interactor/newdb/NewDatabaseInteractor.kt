package com.ivanovsky.passnotes.domain.interactor.newdb

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError.*
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.extensions.toUsedFile

class NewDatabaseInteractor(
    private val dbRepo: EncryptedDatabaseRepository,
    private val usedFileRepository: UsedFileRepository,
    private val fileSystemResolver: FileSystemResolver
) {

    fun createNewDatabaseAndOpen(
        key: KeepassDatabaseKey,
        file: FileDescriptor
    ): OperationResult<Boolean> {
        val result = OperationResult<Boolean>()

        val provider = fileSystemResolver.resolveProvider(file.fsAuthority)

        // TODO: refactor
        val existsResult = provider.exists(file)
        if (existsResult.isSucceeded) {
            val isExists = existsResult.obj

            if (!isExists) {
                val creationResult = dbRepo.createNew(key, file)

                if (creationResult.isSucceeded) {
                    val getFileResult = getFile(file)

                    if (getFileResult.isSucceeded) {
                        val newFile = getFileResult.obj

                        val time = System.currentTimeMillis()
                        val usedFile = newFile.toUsedFile(
                            addedTime = time,
                            lastAccessTime = time
                        )

                        usedFileRepository.insert(usedFile)

                        val openResult = dbRepo.open(key, file, FSOptions.DEFAULT)
                        if (openResult.isSucceededOrDeferred) {
                            result.obj = true
                        } else {
                            result.error = openResult.error
                        }
                    } else {
                        result.error = getFileResult.error
                    }
                } else if (creationResult.isDeferred) {
                    result.error = newFileAccessError(MESSAGE_DEFERRED_OPERATIONS_ARE_NOT_SUPPORTED)
                } else {
                    result.error = creationResult.error
                }
            } else {
                result.error = newFileIsAlreadyExistsError()
            }
        } else {
            result.error = existsResult.error
        }

        return result
    }

    private fun getFile(file: FileDescriptor): OperationResult<FileDescriptor> {
        val result = OperationResult<FileDescriptor>()

        val provider = fileSystemResolver.resolveProvider(file.fsAuthority)
        val getFileResult = provider.getFile(file.path, FSOptions.DEFAULT)

        if (getFileResult.isSucceeded) {
            result.obj = getFileResult.obj
        } else if (getFileResult.isDeferred) {
            result.error = newFileAccessError(MESSAGE_DEFERRED_OPERATIONS_ARE_NOT_SUPPORTED)
        } else {
            result.error = getFileResult.error
        }

        return result
    }
}