package com.ivanovsky.passnotes.domain.interactor.filepicker

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver

class FilePickerInteractor(private val fileSystemResolver: FileSystemResolver) {

    fun getFileList(dir: FileDescriptor): OperationResult<List<FileDescriptor>> {
        return fileSystemResolver.resolveProvider(dir.fsType).listFiles(dir)
    }

    fun getParent(file: FileDescriptor): OperationResult<FileDescriptor> {
        return fileSystemResolver.resolveProvider(file.fsType).getParent(file)
    }

    fun isStoragePermissionRequired(file: FileDescriptor): OperationResult<Boolean> {
        val provider = fileSystemResolver.resolveProvider(file.fsType)
        return provider.isStoragePermissionRequired(file)
    }
}