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
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.storagelist.StorageListContract.FilePickerArgs
import kotlinx.coroutines.*
import javax.inject.Inject

class StorageListPresenter(
    private val view: StorageListContract.View,
    private val action: Action
) : StorageListContract.Presenter {

    @Inject
    lateinit var interactor: StorageListInteractor

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var fileSystemResolver: FileSystemResolver

    @Inject
    lateinit var resourceHelper: ResourceHelper

    override val storageOptions = MutableLiveData<List<StorageOption>>()
    override val showFilePickerScreenEvent = SingleLiveEvent<FilePickerArgs>()
    override val fileSelectedEvent = SingleLiveEvent<FileDescriptor>()
    override val authActivityStartedEvent = SingleLiveEvent<FSType>()

    private var isDropboxAuthDisplayed = false
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    init {
        Injector.getInstance().appComponent.inject(this)
    }

    override fun start() {
        if (isDropboxAuthDisplayed) {
            // case when user returns to the application after dropbox login
            isDropboxAuthDisplayed = false

            val provider = fileSystemResolver.resolveProvider(FSType.DROPBOX)
            if (provider.authenticator.isAuthenticationRequired) {
                val errorMessage = resourceHelper.getString(R.string.authentication_failed)
                view.screenState = ScreenState.dataWithError(errorMessage)
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
            view.screenState = ScreenState.data()
        }
    }

    override fun destroy() {
        job.cancel()
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
            showFilePickerScreenEvent.call(FilePickerArgs(root, action, false))

        } else if (action == Action.PICK_STORAGE) {
            fileSelectedEvent.call(root)
        }
    }

    private fun onExternalStorageSelected(root: FileDescriptor) {
        showFilePickerScreenEvent.call(FilePickerArgs(root, action, true))
    }

    private fun onDropboxStorageSelected() {
        val provider = fileSystemResolver.resolveProvider(FSType.DROPBOX)

        view.screenState = ScreenState.loading()

        if (provider.authenticator.isAuthenticationRequired) {
            isDropboxAuthDisplayed = true
            authActivityStartedEvent.call(FSType.DROPBOX)

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
        fileSelectedEvent.call(file)
    }

    private fun onDropboxRootLoaded(result: OperationResult<FileDescriptor>) {
        if (result.isSucceededOrDeferred) {
            val rootFile = result.obj
            showFilePickerScreenEvent.call(FilePickerArgs(rootFile, action, true))

        } else {
            val message = errorInteractor.processAndGetMessage(result.error)
            view.screenState = ScreenState.error(message)
        }
    }
}