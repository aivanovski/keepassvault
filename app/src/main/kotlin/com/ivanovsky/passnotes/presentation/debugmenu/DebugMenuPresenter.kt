package com.ivanovsky.passnotes.presentation.debugmenu

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.SettingsRepository
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.debugmenu.DebugMenuInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import java9.util.concurrent.CompletableFuture
import java9.util.function.Supplier
import java.io.File
import java.util.concurrent.Executor
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

	@Inject
	lateinit var executor: Executor

	override val screenState = MutableLiveData<ScreenState>()
	override val writeButtonEnabled = MutableLiveData<Boolean>()
	override val openDbButtonEnabled = MutableLiveData<Boolean>()
	override val closeDbButtonEnabled = MutableLiveData<Boolean>()
	override val addEntryButtonEnabled = MutableLiveData<Boolean>()
	override val externalStorageCheckBoxChecked = MutableLiveData<Boolean>()
	override val snackbarMessageAction = SingleLiveAction<String>()

    @Volatile private var lastReadDescriptor: FileDescriptor? = null
	@Volatile private var lastReadFile: File? = null

	init {
		Injector.getInstance().appComponent.inject(this)
	}

	override fun start() {
		screenState.value = ScreenState.data()

		writeButtonEnabled.value = false
		openDbButtonEnabled.value = false
		closeDbButtonEnabled.value = false
		addEntryButtonEnabled.value = false
		externalStorageCheckBoxChecked.value = settings.isExternalStorageCacheEnabled
	}

	override fun stop() {
	}

	override fun onReadButtonClicked(inFile: FileDescriptor) {
		screenState.value = ScreenState.data()

		CompletableFuture.supplyAsync(Supplier {
			interactor.getFileContent(inFile)
		}, executor)
				.thenAccept { result -> onGetFileContentResult(result) }
	}

	private fun onGetFileContentResult(result: OperationResult<Pair<FileDescriptor, File>>) {
		if (result.isSucceededOrDeferred) {
			onDbFileAvailable(result.obj.first, result.obj.second)

			snackbarMessageAction.postCall("File read")
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.postValue(ScreenState.dataWithError(message))
		}
	}

	private fun onDbFileAvailable(fileDescriptor: FileDescriptor, file: File) {
		lastReadDescriptor = fileDescriptor
		lastReadFile = file

		writeButtonEnabled.postValue(true)
		openDbButtonEnabled.postValue(true)
	}

	override fun onWriteButtonClicked() {
		screenState.value = ScreenState.data()

		if (isFileSelected()) {
			CompletableFuture.supplyAsync(Supplier {
				val file = lastReadFile!!
				val descriptor = lastReadDescriptor!!

				descriptor.modified = file.lastModified()

				interactor.writeDbFile(file, descriptor)
			}, executor)
					.thenAccept { result -> onWriteDbFileResult(result) }
		} else {
			screenState.value = ScreenState.dataWithError("File is not loaded")
		}
	}

	private fun onWriteDbFileResult(result: OperationResult<Pair<FileDescriptor, File>>) {
		if (result.isSucceededOrDeferred) {
			onDbFileAvailable(result.obj.first, result.obj.second)

			snackbarMessageAction.postCall("File wrote")
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.postValue(ScreenState.dataWithError(message))
		}
	}

	override fun onNewButtonClicked(password: String, outFile: FileDescriptor) {
		screenState.value = ScreenState.data()

		CompletableFuture.supplyAsync(Supplier {
			interactor.newDbFile(defaultPasswordIfEmpty(password), outFile)
		}, executor)
				.thenAccept { result -> onCreateNewDbFileResult(result) }
	}

	private fun defaultPasswordIfEmpty(password: String): String {
		return if (password.isNotEmpty()) password else "abc123"
	}

	private fun onCreateNewDbFileResult(result: OperationResult<Pair<FileDescriptor, File>>) {
		if (result.isSucceededOrDeferred) {
			onDbFileAvailable(result.obj.first, result.obj.second)

			snackbarMessageAction.postCall("New file created")
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.postValue(ScreenState.dataWithError(message))
		}
	}

	override fun onOpenDbButtonClicked(password: String) {
		screenState.value = ScreenState.data()

		if (isFileSelected()) {
			CompletableFuture.supplyAsync(Supplier {
				interactor.openDbFile(defaultPasswordIfEmpty(password), lastReadFile!!)
			}, executor)
					.thenAccept { result -> onOpenDbResult(result) }
		} else {
			screenState.value = ScreenState.dataWithError("File is not loaded")
		}
	}

	private fun onOpenDbResult(result: OperationResult<Boolean>) {
		if (result.isSucceededOrDeferred) {
			onDbOpened()

			snackbarMessageAction.postCall("DB opened")
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.postValue(ScreenState.dataWithError(message))
		}
	}

	private fun onDbOpened() {
		openDbButtonEnabled.postValue(false)
		closeDbButtonEnabled.postValue(true)
		addEntryButtonEnabled.postValue(true)
	}

	private fun isFileSelected(): Boolean {
		return lastReadFile != null && lastReadDescriptor != null
	}

	override fun onCloseDbButtonClicked() {
		screenState.value = ScreenState.data()

		CompletableFuture.supplyAsync(Supplier {
			interactor.closeDbFile(lastReadFile!!)
		}, executor)
				.thenAccept { result -> onCloseDbFileResult(result) }
	}

	private fun onCloseDbFileResult(result: OperationResult<Boolean>) {
		if (result.isSucceededOrDeferred) {
			onDbClosed()

			snackbarMessageAction.postCall("DB closed")
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.postValue(ScreenState.dataWithError(message))
		}
	}

	private fun onDbClosed() {
		openDbButtonEnabled.postValue(true)
		closeDbButtonEnabled.postValue(false)
		addEntryButtonEnabled.postValue(false)
	}

	override fun onAddEntryButtonClicked() {
		screenState.value = ScreenState.data()

		CompletableFuture.supplyAsync(Supplier {
			interactor.addEntryToDb()
		}, executor)
				.thenAccept { result -> onAddEntryResult(result) }
	}

	private fun onAddEntryResult(result: OperationResult<Boolean>) {
		if (result.isSucceededOrDeferred) {
			snackbarMessageAction.postCall("Entry added")
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.postValue(ScreenState.dataWithError(message))
		}
	}

	override fun onExternalStorageCheckedChanged(isChecked: Boolean) {
		settings.isExternalStorageCacheEnabled = isChecked
	}
}