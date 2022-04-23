package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_READ_KEY_FILE
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import timber.log.Timber
import java.lang.Exception

class FileKeepassKey(
    val file: FileDescriptor,
    private val fileSystemProvider: FileSystemProvider
) : EncryptedDatabaseKey {

    override val type: KeyType
        get() = KeyType.KEY_FILE

    override fun getKey(): OperationResult<ByteArray> {
        val inputResult =
            fileSystemProvider.openFileForRead(file, OnConflictStrategy.CANCEL, FSOptions.READ_ONLY)
        if (inputResult.isFailed) {
            return inputResult.takeError()
        }

        return try {
            val input = inputResult.obj
            val bytes = input.use {
                input.readBytes()
            }
            OperationResult.success(bytes)
        } catch (e: Exception) {
            Timber.d(e)
            OperationResult.error(newGenericIOError(MESSAGE_FAILED_TO_READ_KEY_FILE, e))
        }
    }
}