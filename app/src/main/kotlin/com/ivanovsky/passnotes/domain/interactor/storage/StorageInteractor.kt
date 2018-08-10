package com.ivanovsky.passnotes.domain.interactor.storage

import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.domain.entity.StorageOption

class StorageInteractor {

	fun getAvailableStorageOptions(): List<StorageOption> {
		return listOf(StorageOption(FSType.REGULAR_FS, "Regular File"),
				StorageOption(FSType.DROPBOX, "Dropbox"))
	}
}