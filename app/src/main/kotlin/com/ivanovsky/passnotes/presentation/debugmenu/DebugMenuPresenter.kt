package com.ivanovsky.passnotes.presentation.debugmenu

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.SettingsRepository
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.debugmenu.DebugMenuInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

class DebugMenuPresenter(private val view: DebugMenuContract.View) :
		DebugMenuContract.Presenter {

	@Inject
	lateinit var interactor: DebugMenuInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	@Inject
	lateinit var fileSystemResolver: FileSystemResolver

	@Inject
	lateinit var settings: SettingsRepository

	override val writeButtonEnabled = MutableLiveData<Boolean>()
	override val openDbButtonEnabled = MutableLiveData<Boolean>()
	override val closeDbButtonEnabled = MutableLiveData<Boolean>()
	override val addEntryButtonEnabled = MutableLiveData<Boolean>()
	override val externalStorageCheckBoxChecked = MutableLiveData<Boolean>()

	private var lastReadDescriptor: FileDescriptor? = null
	private var lastReadFile: File? = null
	private val job = Job()
	private val scope = CoroutineScope(Dispatchers.Main + job)

	init {
		Injector.getInstance().appComponent.inject(this)
	}

	override fun start() {
		if (view.screenState.isNotInitialized) {
			view.screenState = ScreenState.data()

			writeButtonEnabled.value = false
			openDbButtonEnabled.value = false
			closeDbButtonEnabled.value = false
			addEntryButtonEnabled.value = false
			externalStorageCheckBoxChecked.value = settings.isExternalStorageCacheEnabled
		}
	}

	override fun destroy() {
		job.cancel()
	}

	override fun onReadButtonClicked(inFile: FileDescriptor) {
		view.screenState = ScreenState.data()

		scope.launch {
			val result = withContext(Dispatchers.Default) {
				interactor.getFileContent(inFile)
			}

			if (result.isSucceededOrDeferred) {
				onDbFileAvailable(result.obj.first, result.obj.second)

                view.showSnackbarMessage("File read")
			} else {
				val message = errorInteractor.processAndGetMessage(result.error)
				view.screenState = ScreenState.dataWithError(message)
			}
		}
	}

	private fun onDbFileAvailable(fileDescriptor: FileDescriptor, file: File) {
		lastReadDescriptor = fileDescriptor
		lastReadFile = file

		writeButtonEnabled.value = true
		openDbButtonEnabled.value = true
	}

	override fun onWriteButtonClicked() {
		view.screenState = ScreenState.data()

		if (isFileSelected()) {
			scope.launch {
				val result = withContext(Dispatchers.Default) {
					val file = lastReadFile!!
					val descriptor = lastReadDescriptor!!

					descriptor.modified = file.lastModified()

					interactor.writeDbFile(file, descriptor)
				}

				if (result.isSucceededOrDeferred) {
					onDbFileAvailable(result.obj.first, result.obj.second)

					view.showSnackbarMessage("File wrote")
				} else {
					val message = errorInteractor.processAndGetMessage(result.error)
					view.screenState = ScreenState.dataWithError(message)
				}
			}
		} else {
			view.screenState = ScreenState.dataWithError("File is not loaded")
		}
	}

	override fun onNewButtonClicked(password: String, outFile: FileDescriptor) {
		view.screenState = ScreenState.data()

		scope.launch {
			val result = withContext(Dispatchers.Default) {
				interactor.newDbFile(defaultPasswordIfEmpty(password), outFile)
			}

			if (result.isSucceededOrDeferred) {
				onDbFileAvailable(result.obj.first, result.obj.second)

				view.showSnackbarMessage("New file created")
			} else {
				val message = errorInteractor.processAndGetMessage(result.error)
				view.screenState = ScreenState.dataWithError(message)
			}
		}
	}

	private fun defaultPasswordIfEmpty(password: String): String {
		return if (password.isNotEmpty()) password else "abc123"
	}

	override fun onOpenDbButtonClicked(password: String) {
		view.screenState = ScreenState.data()

		if (isFileSelected()) {
			scope.launch {
				val result = withContext(Dispatchers.Default) {
					interactor.openDbFile(defaultPasswordIfEmpty(password), lastReadFile!!)
				}

				if (result.isSucceededOrDeferred) {
					onDbOpened()

					view.showSnackbarMessage("DB opened")
				} else {
					val message = errorInteractor.processAndGetMessage(result.error)
					view.screenState = ScreenState.dataWithError(message)
				}
			}
		} else {
			view.screenState = ScreenState.dataWithError("File is not loaded")
		}
	}

	private fun onDbOpened() {
		openDbButtonEnabled.value = false
		closeDbButtonEnabled.value = true
		addEntryButtonEnabled.value = true
	}

	private fun isFileSelected(): Boolean {
		return lastReadFile != null && lastReadDescriptor != null
	}

	override fun onCloseDbButtonClicked() {
		view.screenState = ScreenState.data()

		scope.launch {
			val result = withContext(Dispatchers.Default) {
				interactor.closeDbFile(lastReadFile!!)
			}

			if (result.isSucceededOrDeferred) {
				onDbClosed()

				view.showSnackbarMessage("DB closed")
			} else {
				val message = errorInteractor.processAndGetMessage(result.error)
				view.screenState = ScreenState.dataWithError(message)
			}
		}
	}

	private fun onDbClosed() {
		openDbButtonEnabled.value = true
		closeDbButtonEnabled.value = false
		addEntryButtonEnabled.value = false
	}

	override fun onAddEntryButtonClicked() {
		view.screenState = ScreenState.data()

		scope.launch {
			val result = withContext(Dispatchers.Default) {
				interactor.addEntryToDb()
			}

			if (result.isSucceededOrDeferred) {
				view.showSnackbarMessage("Entry added")
			} else {
				val message = errorInteractor.processAndGetMessage(result.error)
				view.screenState = ScreenState.dataWithError(message)
			}
		}
	}

	override fun onExternalStorageCheckedChanged(isChecked: Boolean) {
		settings.isExternalStorageCacheEnabled = isChecked
	}
}