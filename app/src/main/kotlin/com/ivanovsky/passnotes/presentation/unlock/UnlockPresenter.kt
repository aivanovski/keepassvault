package com.ivanovsky.passnotes.presentation.unlock

import android.arch.lifecycle.MutableLiveData
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

import javax.inject.Inject

import io.reactivex.disposables.CompositeDisposable

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

	override val recentlyUsedFiles = MutableLiveData<List<FileDescriptor>>()
	override val selectedRecentlyUsedFile = MutableLiveData<FileDescriptor>()
	override val screenState = MutableLiveData<ScreenState>()
	override val showGroupsScreenAction = SingleLiveAction<Void>()
	override val showNewDatabaseScreenAction = SingleLiveAction<Void>()
	override val hideKeyboardAction = SingleLiveAction<Void>()
	override val showOpenFileScreenAction = SingleLiveAction<Void>()
	override val showSettingsScreenAction = SingleLiveAction<Void>()
	override val showAboutScreenAction = SingleLiveAction<Void>()
	override val snackbarMessageAction = SingleLiveAction<String>()
	private val disposables = CompositeDisposable()
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
		disposables.clear()
	}

	override fun loadData(selectedFile: FileDescriptor?) {
		val disposable = interactor.getRecentlyOpenedFiles()
				.subscribe { result -> onGetRecentlyOpenedFilesResult(result, selectedFile) }

		disposables.add(disposable)
	}

	private fun onGetRecentlyOpenedFilesResult(result: OperationResult<List<FileDescriptor>>,
	                                           selectedFile: FileDescriptor?) {
		if (result.isSuccessful) {
			val files = result.result
			if (files.isNotEmpty()) {
				recentlyUsedFiles.value = files

				if (selectedFile != null) {
					val selectedPosition = files.indexOfFirst { f -> isFileEqualsByUidAndFsType(f, selectedFile) }
					if (selectedPosition != -1) {
						selectedRecentlyUsedFile.value = selectedFile
					}
				}

				screenState.value = ScreenState.data()
			} else {
				screenState.value = ScreenState.empty(context.getString(R.string.no_files_to_open))
			}
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.setValue(ScreenState.error(message))
		}
	}

	private fun isFileEqualsByUidAndFsType(lhs: FileDescriptor, rhs: FileDescriptor): Boolean {
		return lhs.uid == rhs.uid && lhs.fsType == rhs.fsType
	}

	override fun onUnlockButtonClicked(password: String, file: FileDescriptor) {
		hideKeyboardAction.call()
		screenState.value = ScreenState.loading()

		val key = KeepassDatabaseKey(password)

		val disposable = interactor.openDatabase(key, file)
				.subscribe { result -> onOpenDatabaseResult(result) }

		disposables.add(disposable)
	}

	private fun onOpenDatabaseResult(result: OperationResult<Boolean>) {
		if (result.result != null) {
			showGroupsScreenAction.call()
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.value = ScreenState.error(message)
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

	override fun onFilePicked(file: FileDescriptor) {
		//called when user select file from built-in file picker
		screenState.value = ScreenState.loading()

		val usedFile = UsedFile()

		usedFile.filePath = file.path
		usedFile.fileUid = file.uid
		usedFile.fsType = file.fsType

		val disposable = interactor.saveUsedFileWithoutAccessTime(usedFile)
				.subscribe { result -> onPickedFileSaved(result, file) }

		disposables.add(disposable)
	}

	private fun onPickedFileSaved(result: OperationResult<Boolean>, file: FileDescriptor) {
		if (result.isSuccessful) {
			loadData(file)

		} else {
			screenState.value = ScreenState.data()

			val message = errorInteractor.processAndGetMessage(result.error)
			snackbarMessageAction.call(message)
		}
	}

	override fun onFileSelectedByUser(file: FileDescriptor) {
		selectedFile = file
		selectedRecentlyUsedFile.value = file
	}
}