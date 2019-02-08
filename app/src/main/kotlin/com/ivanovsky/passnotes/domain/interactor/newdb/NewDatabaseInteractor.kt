package com.ivanovsky.passnotes.domain.interactor.newdb

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.*
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.encdb.exception.EncryptedDatabaseException
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.util.Logger

open class NewDatabaseInteractor(private val dbRepository: EncryptedDatabaseRepository,
                            private val usedFileRepository: UsedFileRepository,
                            private val fileSystemResolver: FileSystemResolver,
                            private val observerBus: ObserverBus) {

	fun createNewDatabaseAndOpen(key: KeepassDatabaseKey, file: FileDescriptor): OperationResult<Boolean> {
		val result = OperationResult<Boolean>()

		val provider = fileSystemResolver.resolveProvider(file.fsType)

		try {
			if (provider.exists(file)) {
				result.error = newFileIsAlreadyExistsError()
			}
		} catch (e: FileSystemException) {
			result.error = newGenericIOError(OperationError.MESSAGE_FAILED_TO_LOAD_FILE, e)
		}

		if (result.error == null) {//TODO: could I make it look better?
			if (dbRepository.createNew(key, file)) {
				val usedFile = UsedFile()

				usedFile.filePath = file.path
				usedFile.fileUid = file.uid
				usedFile.fsType = file.fsType
				usedFile.lastAccessTime = System.currentTimeMillis()

				usedFileRepository.insert(usedFile)

				observerBus.notifyUsedFileDataSetChanged()

				try {
					val db = dbRepository.open(key, file)

					Injector.getInstance().createEncryptedDatabaseComponent(db)

					result.result = true
				} catch (e: EncryptedDatabaseException) {
					Logger.printStackTrace(e)

					result.error = newGenericError(OperationError.MESSAGE_UNKNOWN_ERROR, e)
				}

			} else {
				result.setError(newGenericError(OperationError.MESSAGE_UNKNOWN_ERROR))
			}
		}

		return result
	}
}