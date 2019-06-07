package com.ivanovsky.passnotes.presentation.unlock

import androidx.lifecycle.MutableLiveData
import android.content.Context

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.presentation.core.FragmentState
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import java9.util.concurrent.CompletableFuture.supplyAsync
import java9.util.function.Supplier
import java.util.concurrent.Executor

import javax.inject.Inject

class UnlockPresenter(private val context: Context,
                       private val view: UnlockContract.View) :
		UnlockContract.Presenter,
		ObserverBus.UsedFileDataSetObserver {

	@Inject
	lateinit var interactor: UnlockInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	@Inject
	lateinit var observerBus: ObserverBus

	@Inject
	lateinit var executor: Executor

	override val recentlyUsedFiles = MutableLiveData<List<FileDescriptor>>()
	override val selectedRecentlyUsedFile = MutableLiveData<FileDescriptor>()
	override val screenState = MutableLiveData<ScreenState>()
	override val showGroupsScreenAction = SingleLiveAction<Void>()
	override val showNewDatabaseScreenAction = SingleLiveAction<Void>()
	override val hideKeyboardAction = SingleLiveAction<Void>()
	override val showOpenFileScreenAction = SingleLiveAction<Void>()
	override val showSettingsScreenAction = SingleLiveAction<Void>()
	override val showAboutScreenAction = SingleLiveAction<Void>()
	override val showDebugMenuScreenAction = SingleLiveAction<Void>()
	override val snackbarMessageAction = SingleLiveAction<String>()
	private var selectedFile: FileDescriptor? = null

	init {
		Injector.getInstance().appComponent.inject(this)
	}

	override fun start() {
		view.setState(FragmentState.LOADING)
		observerBus.register(this)
		loadData(null)
	}

	override fun stop() {
		observerBus.unregister(this)
	}

	override fun loadData(selectedFile: FileDescriptor?) {
		supplyAsync(Supplier { interactor.getRecentlyOpenedFiles() }, executor)
				.thenAccept { data -> onLoadRecentlyOpenedFilesResult(selectedFile, data) }
	}

	private fun onLoadRecentlyOpenedFilesResult(selectedFile: FileDescriptor?, result: OperationResult<List<FileDescriptor>>) {
		if (result.isSucceededOrDeferred) {
			val files = result.obj
			if (files.isNotEmpty()) {
				recentlyUsedFiles.postValue(files)

				if (selectedFile != null) {
					val selectedPosition = files.indexOfFirst { f -> isFileEqualsByUidAndFsType(f, selectedFile) }
					if (selectedPosition != -1) {
						selectedRecentlyUsedFile.postValue(selectedFile)
					}
				}

				screenState.postValue(ScreenState.data())
			} else {
				screenState.postValue(ScreenState.empty(context.getString(R.string.no_files_to_open)))
			}
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.postValue(ScreenState.error(message))
		}
	}

	private fun isFileEqualsByUidAndFsType(lhs: FileDescriptor, rhs: FileDescriptor): Boolean {
		return lhs.uid == rhs.uid && lhs.fsType == rhs.fsType
	}

	override fun onUnlockButtonClicked(password: String, file: FileDescriptor) {
		hideKeyboardAction.call()
		screenState.value = ScreenState.loading()

		val key = KeepassDatabaseKey(password)

		supplyAsync(Supplier { interactor.openDatabase(key, file) }, executor)
				.thenAccept { result -> onOpenDatabaseResult(result) }
	}

	private fun onOpenDatabaseResult(result: OperationResult<Boolean>) {
		if (result.isSucceededOrDeferred) {
			showGroupsScreenAction.postCall()
			screenState.postValue(ScreenState.data())
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.postValue(ScreenState.error(message))
		}
	}

	override fun onUsedFileDataSetChanged() {
		loadData(null)
	}

	override fun onOpenFileMenuClicked() {
		showOpenFileScreenAction.call()
	}

	override fun onSettingsMenuClicked() {
		showSettingsScreenAction.call()
	}

	override fun onAboutMenuClicked() {
		showAboutScreenAction.call()
	}

	override fun onDebugMenuClicked() {
		showDebugMenuScreenAction.call()
	}

	override fun onFilePicked(file: FileDescriptor) {
		//called when user select file from built-in file picker
		screenState.value = ScreenState.loading()

		val usedFile = UsedFile()

		usedFile.filePath = file.path
		usedFile.fileUid = file.uid
		usedFile.fsType = file.fsType

		supplyAsync(Supplier { interactor.saveUsedFileWithoutAccessTime(usedFile) }, executor)
				.thenAccept { result -> onSaveUsedFileResult(file, result) }
	}

	private fun onSaveUsedFileResult(selectedFile: FileDescriptor, result: OperationResult<Boolean>) {
		if (result.isSucceededOrDeferred) {
			loadData(selectedFile)

		} else {
			screenState.postValue(ScreenState.data())

			val message = errorInteractor.processAndGetMessage(result.error)
			snackbarMessageAction.postCall(message)
		}
	}

	override fun onFileSelectedByUser(file: FileDescriptor) {
		selectedFile = file
		selectedRecentlyUsedFile.value = file
	}
}