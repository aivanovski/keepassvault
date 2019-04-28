package com.ivanovsky.passnotes.domain.interactor.filepicker

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class FilePickerInteractor(private val fileSystemResolver: FileSystemResolver) {

	fun getFileList(dir: FileDescriptor): OperationResult<List<FileDescriptor>> {
		return fileSystemResolver.resolveProvider(dir.fsType).listFiles(dir)
	}

	fun getParent(file: FileDescriptor): OperationResult<FileDescriptor> {
		return fileSystemResolver.resolveProvider(file.fsType).getParent(file)
	}
}