package com.ivanovsky.passnotes.domain.interactor.unlock

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError.*
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.injection.Injector

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class UnlockInteractor(private val fileRepository: UsedFileRepository,
                       private val dbRepository: EncryptedDatabaseRepository,
                       private val observerBus: ObserverBus) {

	fun getRecentlyOpenedFiles(): Single<OperationResult<List<FileDescriptor>>> {
		return Single.fromCallable<List<FileDescriptor>> { loadAndSortUsedFiles()}
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.map { files -> OperationResult.success(files) }
	}

	private fun loadAndSortUsedFiles(): List<FileDescriptor> {
		return fileRepository.allUsedFiles
				.sortedByDescending { file -> if (file.lastAccessTime != null) file.lastAccessTime else file.addedTime }
				.map { file ->  createFileDescriptor(file)}
	}

	private fun createFileDescriptor(usedFile: UsedFile): FileDescriptor {
		val file = FileDescriptor()

		file.uid = usedFile.fileUid
		file.path = usedFile.filePath
		file.fsType = usedFile.fsType

		return file
	}

	fun openDatabase(key: KeepassDatabaseKey, file: FileDescriptor): Single<OperationResult<Boolean>> {
		return Single.fromCallable<EncryptedDatabase> { openDatabaseAsync(key, file) }
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.map { db ->
					Injector.getInstance().createEncryptedDatabaseComponent(db)
					OperationResult.success(true)
				}
				.onErrorResumeNext { throwable -> Single.just(makeResultFromThrowable(throwable)) }
	}

	private fun openDatabaseAsync(key: KeepassDatabaseKey, file: FileDescriptor): EncryptedDatabase {
		val db = dbRepository.open(key, file)

		val usedFile = fileRepository.findByUidAndFsType(file.uid, file.fsType)
		if (usedFile != null) {
			usedFile.lastAccessTime = System.currentTimeMillis()

			fileRepository.update(usedFile)

			observerBus.notifyUsedFileDataSetChanged()
		}

		return db
	}

	private fun makeResultFromThrowable(throwable: Throwable): OperationResult<Boolean> {
		val result = OperationResult<Boolean>()

		result.result = false
		result.error = newGenericError(MESSAGE_UNKNOWN_ERROR, throwable)

		return result
	}

	fun saveUsedFileWithoutAccessTime(file: UsedFile): Single<OperationResult<Boolean>> {
		return Single.fromCallable { saveUsedFileWithoutAccessTimeAsync(file) }
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.onErrorResumeNext { throwable -> Single.just(makeResultFromThrowable(throwable))}
	}

	private fun saveUsedFileWithoutAccessTimeAsync(file: UsedFile): OperationResult<Boolean> {
		val result = OperationResult<Boolean>()

		val existing = fileRepository.findByUidAndFsType(file.fileUid, file.fsType)
		if (existing == null) {
			file.lastAccessTime = null

			fileRepository.insert(file)
			observerBus.notifyUsedFileDataSetChanged()

			result.result = true
		} else {
			result.result = false
			result.error = newDbError(MESSAGE_RECORD_IS_ALREADY_EXISTS)
		}

		return result
	}
}
