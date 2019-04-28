package com.ivanovsky.passnotes.presentation.storagelist

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.domain.entity.StorageOptionType
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import com.ivanovsky.passnotes.presentation.storagelist.StorageListContract.FilePickerArgs
import com.ivanovsky.passnotes.util.COROUTINE_EXCEPTION_HANDLER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StorageListPresenter(private val action: Action) :
		StorageListContract.Presenter {

	@Inject
	lateinit var interactor: StorageListInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	@Inject
	lateinit var fileSystemResolver: FileSystemResolver

	@Inject
	lateinit var resourceHelper: ResourceHelper

	override val storageOptions = MutableLiveData<List<StorageOption>>()
	override val screenState = MutableLiveData<ScreenState>()
	override val showFilePickerScreenAction = SingleLiveAction<FilePickerArgs>()
	override val fileSelectedAction = SingleLiveAction<FileDescriptor>()
	override val authActivityStartedAction = SingleLiveAction<FSType>()

	private var isDropboxAuthDisplayed = false
	private val scope = CoroutineScope(Dispatchers.Main + COROUTINE_EXCEPTION_HANDLER)

	init {
		Injector.getInstance().appComponent.inject(this)
	}

	override fun start() {
		if (isDropboxAuthDisplayed) {
			// case when user returns to the application after dropbox login
			isDropboxAuthDisplayed = false

			val provider = fileSystemResolver.resolveProvider(FSType.DROPBOX)
			if (provider.authenticator.isAuthenticationRequired) {
				screenState.value = ScreenState.dataWithError(
						resourceHelper.getString(R.string.authentication_failed))
			} else {
				scope.launch {
					val dropboxRoot = withContext(Dispatchers.Default) {
						interactor.getDropboxRoot()
					}

					onDropboxRootLoaded(dropboxRoot)
				}
			}

		} else {
			storageOptions.value = interactor.getAvailableStorageOptions()
			screenState.value = ScreenState.data()
		}
	}

	override fun stop() {
	}

	override fun onStorageOptionClicked(option: StorageOption) {
		when (option.type) {
			StorageOptionType.PRIVATE_STORAGE -> onPrivateStorageSelected(option.root!!)
			StorageOptionType.EXTERNAL_STORAGE -> onExternalStorageSelected(option.root!!)
			StorageOptionType.DROPBOX -> onDropboxStorageSelected()
		}
	}

	private fun onPrivateStorageSelected(root: FileDescriptor) {
		if (action == Action.PICK_FILE) {
			showFilePickerScreenAction.call(FilePickerArgs(root, action, false))

		} else if (action == Action.PICK_STORAGE) {
			fileSelectedAction.call(root)
		}
	}

	private fun onExternalStorageSelected(root: FileDescriptor) {
		showFilePickerScreenAction.call(FilePickerArgs(root, action, true))
	}

	private fun onDropboxStorageSelected() {
		val provider = fileSystemResolver.resolveProvider(FSType.DROPBOX)

		screenState.value = ScreenState.loading()

		if (provider.authenticator.isAuthenticationRequired) {
			isDropboxAuthDisplayed = true
			authActivityStartedAction.call(FSType.DROPBOX)

		} else {
			scope.launch {
				val dropboxRoot = withContext(Dispatchers.Default) {
					interactor.getDropboxRoot()
				}

				onDropboxRootLoaded(dropboxRoot)
			}
		}
	}

	override fun onFilePicked(file: FileDescriptor) {
		fileSelectedAction.call(file)
	}

	private fun onDropboxRootLoaded(result: OperationResult<FileDescriptor>) {
		if (result.isSucceededOrDeferred) {
			val rootFile = result.obj
			showFilePickerScreenAction.call(FilePickerArgs(rootFile, action, true))

		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.value = ScreenState.error(message)
		}
	}
}