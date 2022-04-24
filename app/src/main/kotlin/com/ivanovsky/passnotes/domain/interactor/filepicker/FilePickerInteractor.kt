package com.ivanovsky.passnotes.domain.interactor.filepicker

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.DispatcherProvider
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
}