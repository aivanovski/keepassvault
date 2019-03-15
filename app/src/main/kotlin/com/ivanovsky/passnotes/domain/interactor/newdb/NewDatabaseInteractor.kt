package com.ivanovsky.passnotes.domain.interactor.newdb

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError.*
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.injection.Injector

open class NewDatabaseInteractor(private val dbRepository: EncryptedDatabaseRepository,
                            private val usedFileRepository: UsedFileRepository,
                            private val fileSystemResolver: FileSystemResolver,
                            private val observerBus: ObserverBus) {

	fun createNewDatabaseAndOpen(key: KeepassDatabaseKey, file: FileDescriptor): OperationResult<Boolean> {
		val result = OperationResult<Boolean>()

		val provider = fileSystemResolver.resolveProvider(file.fsType)

		val existsResult = provider.exists(file)
		if (existsResult.isSucceeded) {
			val exists = existsResult.obj

			if (!exists) {
				val creationResult = dbRepository.createNew(key, file)

				if (creationResult.isSucceededOrDeferred) {
					val usedFile = UsedFile()

					usedFile.filePath = file.path
					usedFile.fileUid = file.uid
					usedFile.fsType = file.fsType
					usedFile.lastAccessTime = System.currentTimeMillis()

					usedFileRepository.insert(usedFile)

					observerBus.notifyUsedFileDataSetChanged()

					val openResult = dbRepository.open(key, file)
					if (openResult.isSucceededOrDeferred) {
						val db = openResult.obj

						Injector.getInstance().createEncryptedDatabaseComponent(db)

						result.obj = true
					} else {
						result.error = openResult.error
					}
				} else {
					result.error = creationResult.error
				}
			} else {
				result.error = newFileIsAlreadyExistsError()
			}
		} else {
			result.error = existsResult.error
		}

		return result
	}
}