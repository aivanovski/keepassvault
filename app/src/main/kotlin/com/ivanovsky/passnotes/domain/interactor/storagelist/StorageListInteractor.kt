package com.ivanovsky.passnotes.domain.interactor.storagelist

import android.content.Context
import android.os.Environment
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.*

class StorageListInteractor(private val context: Context) {

	fun getAvailableStorageOptions(): List<StorageOption> {
		return listOf(createPrivateStorageOption(),
				createExternalStorageOption(),
				createDropboxOption())
	}

	private fun createPrivateStorageOption(): StorageOption {
		return StorageOption(PRIVATE_STORAGE,
				context.getString(R.string.private_app_storage),
				createPrivateStorageDir())
	}

	private fun createExternalStorageOption(): StorageOption {
		return StorageOption(EXTERNAL_STORAGE,
				context.getString(R.string.external_storage),
				createExternalStorageDir())
	}

	private fun createDropboxOption(): StorageOption {
		return StorageOption(DROPBOX,
				context.getString(R.string.dropbox),
				null)
	}

	private fun createPrivateStorageDir(): FileDescriptor {
		return FileDescriptor.fromRegularFile(context.filesDir)
	}

	private fun createExternalStorageDir(): FileDescriptor {
		return FileDescriptor.fromRegularFile(Environment.getExternalStorageDirectory())
	}
}