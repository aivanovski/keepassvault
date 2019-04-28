package com.ivanovsky.passnotes.domain

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.file.SyncStrategy

class FileSyncHelper(private val fileSystemResolver: FileSystemResolver) {

	fun getModifiedFileByUid(uid: String, fsType: FSType): FileDescriptor? {
		val provider = fileSystemResolver.resolveProvider(fsType)

		val modifiedFiles = provider.syncProcessor?.locallyModifiedFiles

		return modifiedFiles?.find { file -> file.uid == uid }
	}

	fun resolve(file: FileDescriptor): OperationResult<FileDescriptor> {
		val provider = fileSystemResolver.resolveProvider(file.fsType)

		return provider.syncProcessor.process(file,
				SyncStrategy.LAST_MODIFICATION_WINS,
				OnConflictStrategy.CANCEL)
	}
}