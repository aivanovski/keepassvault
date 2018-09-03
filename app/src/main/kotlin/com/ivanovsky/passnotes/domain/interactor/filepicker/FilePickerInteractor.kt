package com.ivanovsky.passnotes.domain.interactor.filepicker

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class FilePickerInteractor(private val fileSystemResolver: FileSystemResolver) {

	fun getFileList(dir: FileDescriptor): Single<OperationResult<List<FileDescriptor>>> {
		return Single.fromCallable { fileSystemResolver.resolveProvider(dir.fsType).listFiles(dir) }
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
	}

	fun getParent(file: FileDescriptor): Single<OperationResult<FileDescriptor>> {
		return Single.fromCallable { fileSystemResolver.resolveProvider(file.fsType).getParent(file) }
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
	}
}