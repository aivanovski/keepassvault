package com.ivanovsky.passnotes.domain.interactor.unlock

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError.*
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.injection.Injector

class UnlockInteractor(private val fileRepository: UsedFileRepository,
                       private val dbRepository: EncryptedDatabaseRepository,
                       private val observerBus: ObserverBus) {

	fun getRecentlyOpenedFiles(): OperationResult<List<FileDescriptor>> {
		return OperationResult.success(loadAndSortUsedFiles())
	}

	private fun loadAndSortUsedFiles(): List<FileDescriptor> {
		return fileRepository.all
				.sortedByDescending { file -> if (file.lastAccessTime != null) file.lastAccessTime else file.addedTime }
				.map { file -> createFileDescriptor(file)}
	}

	private fun createFileDescriptor(usedFile: UsedFile): FileDescriptor {
		val file = FileDescriptor()

		file.uid = usedFile.fileUid
		file.path = usedFile.filePath
		file.fsType = usedFile.fsType

		return file
	}

	fun openDatabase(key: KeepassDatabaseKey, file: FileDescriptor): OperationResult<Boolean> {
		val db = dbRepository.open(key, file)

		val usedFile = fileRepository.findByUidAndFsType(file.uid, file.fsType)
		if (usedFile != null) {
			usedFile.lastAccessTime = System.currentTimeMillis()

			fileRepository.update(usedFile)

			observerBus.notifyUsedFileDataSetChanged()

			Injector.getInstance().createEncryptedDatabaseComponent(db)
		}

		return OperationResult.success(true)
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
