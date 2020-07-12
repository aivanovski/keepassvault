package com.ivanovsky.passnotes.domain.interactor.unlock

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_RECORD_IS_ALREADY_EXISTS
import com.ivanovsky.passnotes.data.entity.OperationError.Type.NETWORK_IO_ERROR
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.FileSyncHelper

class UnlockInteractor(
    private val fileRepository: UsedFileRepository,
    private val dbRepository: EncryptedDatabaseRepository,
    private val observerBus: ObserverBus,
    private val fileSyncHelper: FileSyncHelper
) {

    fun hasActiveDatabase(): Boolean {
        return dbRepository.isOpened
    }

    fun closeActiveDatabase(): OperationResult<Unit> {
        if (!dbRepository.isOpened) {
            return OperationResult.success(Unit)
        }

        val closeResult = dbRepository.close()
        return closeResult.takeStatusWith(Unit)
    }

    fun getRecentlyOpenedFiles(): OperationResult<List<FileDescriptor>> {
        return OperationResult.success(loadAndSortUsedFiles())
    }

    private fun loadAndSortUsedFiles(): List<FileDescriptor> {
        return fileRepository.all
            .sortedByDescending { file -> if (file.lastAccessTime != null) file.lastAccessTime else file.addedTime }
            .map { file -> createFileDescriptor(file) }
    }

    private fun createFileDescriptor(usedFile: UsedFile): FileDescriptor {
        val file = FileDescriptor()

        file.uid = usedFile.fileUid
        file.path = usedFile.filePath
        file.fsType = usedFile.fsType

        return file
    }

    fun openDatabase(key: KeepassDatabaseKey, file: FileDescriptor): OperationResult<Boolean> {
        val result = OperationResult<Boolean>()

        var syncError: OperationError? = null
        val locallyModifiedFile = fileSyncHelper.getModifiedFileByUid(file.uid, file.fsType)
        if (locallyModifiedFile != null) {
            val syncResult = fileSyncHelper.resolve(locallyModifiedFile)
            if (syncResult.isFailed && syncResult.error.type != NETWORK_IO_ERROR) {
                syncError = syncResult.error
            }
        }

        if (syncError == null) {
            val openResult = dbRepository.open(key, file)
            if (openResult.isSucceededOrDeferred) {
                updateFileAccessTime(file)
                result.obj = true
            } else {
                result.error = openResult.error
            }
        } else {
            result.error = syncError
        }

        return result
    }

    private fun updateFileAccessTime(file: FileDescriptor) {
        val usedFile = fileRepository.findByUidAndFsType(file.uid, file.fsType)
        if (usedFile != null) {
            usedFile.lastAccessTime = System.currentTimeMillis()

            fileRepository.update(usedFile)

            observerBus.notifyUsedFileDataSetChanged()
        }
    }

    fun saveUsedFileWithoutAccessTime(file: UsedFile): OperationResult<Boolean> {
        val result = OperationResult<Boolean>()

        val existing = fileRepository.findByUidAndFsType(file.fileUid, file.fsType)
        if (existing == null) {
            file.lastAccessTime = null

            fileRepository.insert(file)
            observerBus.notifyUsedFileDataSetChanged()

            result.obj = true
        } else {
            result.obj = false
            result.error = newDbError(MESSAGE_RECORD_IS_ALREADY_EXISTS)
        }

        return result
    }
}
