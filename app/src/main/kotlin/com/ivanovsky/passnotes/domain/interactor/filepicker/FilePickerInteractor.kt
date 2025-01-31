package com.ivanovsky.passnotes.domain.interactor.filepicker

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.util.InputOutputUtils
import java.io.InputStream
import kotlinx.coroutines.withContext

class FilePickerInteractor(
    private val fileSystemResolver: FileSystemResolver,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getFileList(dir: FileDescriptor): OperationResult<List<FileDescriptor>> =
        withContext(dispatchers.IO) {
            fileSystemResolver
                .resolveProvider(dir.fsAuthority)
                .listFiles(dir)
        }

    suspend fun getParent(file: FileDescriptor): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            fileSystemResolver
                .resolveProvider(file.fsAuthority)
                .getParent(file)
        }

    suspend fun copyToPrivateStorage(file: FileDescriptor): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            val inputResult = openFile(file)
            if (inputResult.isFailed) {
                return@withContext inputResult.mapError()
            }

            val fsProvider = fileSystemResolver.resolveProvider(FSAuthority.INTERNAL_FS_AUTHORITY)

            val getRootResult = fsProvider.rootFile
            if (getRootResult.isFailed) {
                return@withContext getRootResult.mapError()
            }

            val root = getRootResult.getOrThrow()
            val path = root.path + "/" + file.name
            val dstFile = FileDescriptor(
                fsAuthority = FSAuthority.INTERNAL_FS_AUTHORITY,
                path = path,
                uid = path,
                name = file.name,
                isDirectory = false,
                isRoot = false
            )

            val outputResult = fsProvider.openFileForWrite(
                dstFile,
                OnConflictStrategy.CANCEL,
                FSOptions.DEFAULT
            )
            if (outputResult.isFailed) {
                return@withContext outputResult.mapError()
            }

            val copyResult = InputOutputUtils.copy(
                from = inputResult.getOrThrow(),
                to = outputResult.getOrThrow(),
                isClose = true
            )
            if (copyResult.isFailed) {
                return@withContext copyResult.mapError()
            }

            fsProvider.getFile(path, FSOptions.DEFAULT)
        }

    private suspend fun openFile(file: FileDescriptor): OperationResult<InputStream> =
        withContext(dispatchers.IO) {
            val fsProvider = fileSystemResolver.resolveProvider(file.fsAuthority)

            fsProvider.openFileForRead(
                file,
                OnConflictStrategy.CANCEL,
                FSOptions.READ_ONLY
            )
        }
}