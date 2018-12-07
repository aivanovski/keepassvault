package com.ivanovsky.passnotes.domain.interactor.storagelist

import android.content.Context
import android.os.Environment
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.*
import com.ivanovsky.passnotes.injection.Injector
import javax.inject.Inject

class StorageListInteractor(private val context: Context) {

	@Inject
	lateinit var fileSystemResolver: FileSystemResolver

	init {
		Injector.getInstance().appComponent.inject(this)
	}

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
				createDropboxStorageDir())
	}

	private fun createPrivateStorageDir(): FileDescriptor {
		return FileDescriptor.fromRegularFile(context.filesDir)
	}

	private fun createExternalStorageDir(): FileDescriptor {
		return FileDescriptor.fromRegularFile(Environment.getExternalStorageDirectory())
	}

	private fun createDropboxStorageDir(): FileDescriptor {
		val file = FileDescriptor()

		file.fsType = FSType.DROPBOX
		file.path = "/"
		file.isDirectory = true
		file.isRoot = true

		return file
	}

	fun getDropboxRoot(): OperationResult<FileDescriptor> {
		val provider = fileSystemResolver.resolveProvider(FSType.DROPBOX)
		return provider.rootFile

	}
}