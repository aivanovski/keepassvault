package com.ivanovsky.passnotes.presentation.storagelist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.domain.entity.StorageOptionType
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.DROPBOX
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.EXTERNAL_STORAGE
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.PRIVATE_STORAGE
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.WEBDAV
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.viewmodels.SingleTextCellViewModel
import com.ivanovsky.passnotes.presentation.filepicker.model.FilePickerArgs
import com.ivanovsky.passnotes.presentation.storagelist.converter.toCellModels
import com.ivanovsky.passnotes.presentation.storagelist.converter.toFilePickerAction
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StorageListViewModel(
    private val interactor: StorageListInteractor,
    private val errorInteractor: ErrorInteractor,
    private val fileSystemResolver: FileSystemResolver,
    private val resourceProvider: ResourceProvider,
    private val dispatchers: DispatcherProvider
) : BaseScreenViewModel() {

    val viewTypes = ViewModelTypes()
        .add(SingleTextCellViewModel::class, R.layout.cell_single_text)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData<ScreenState>(ScreenState.notInitialized())

    val selectFileEvent = SingleLiveEvent<FileDescriptor>()
    val showAuthActivityEvent = SingleLiveEvent<FSAuthority>()
    val showFilePickerScreenEvent = SingleLiveEvent<FilePickerArgs>()

    private val cellFactory = StorageListCellFactory()
    private var storageOptions: List<StorageOption>? = null
    private var requestedAction: Action? = null
    private var isExternalAuthActivityLaunched = false
    private var selectedOption: StorageOption? = null

    init {
        subscribeToEvents()
    }

    fun loadData(requestedAction: Action) {
        this.requestedAction = requestedAction

        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val options = withContext(dispatchers.IO) {
                interactor.getAvailableStorageOptions()
            }

            val cellModels = options.toCellModels()
            setCellElements(cellFactory.createCellViewModels(cellModels, eventProvider))

            storageOptions = options
            screenState.value = ScreenState.data()
        }
    }

    fun onScreenStart() {
        val selectedOption = selectedOption ?: return

        if (isExternalAuthActivityLaunched) {
            isExternalAuthActivityLaunched = false

            val fsAuthority = selectedOption.root.fsAuthority
            val provider = fileSystemResolver.resolveProvider(fsAuthority)
            if (provider.authenticator.isAuthenticationRequired()) {
                val errorMessage = resourceProvider.getString(R.string.authentication_failed)
                screenState.value = ScreenState.dataWithError(errorMessage)
            } else {
                viewModelScope.launch {
                    val fsRoot = interactor.getRemoteFileSystemRoot(fsAuthority)

                    onRemoteRootLoaded(fsRoot)
                }
            }
        }
    }

    fun onFilePickedByPicker(file: FileDescriptor) {
        selectFileEvent.call(file)
    }

    fun onInternalAuthSuccess(fsAuthority: FSAuthority) {
        viewModelScope.launch {
            val remoteFsRoot = interactor.getRemoteFileSystemRoot(fsAuthority)

            onRemoteRootLoaded(remoteFsRoot)
        }
    }

    fun onInternalAuthFailed() {
        screenState.value = ScreenState.data()
    }

    private fun subscribeToEvents() {
        eventProvider.subscribe(this) { event ->
            if (event.containsKey(SingleTextCellViewModel.CLICK_EVENT)) {
                val id = event.getString(SingleTextCellViewModel.CLICK_EVENT) ?: EMPTY
                onStorageOptionClicked(StorageOptionType.valueOf(id))
            }
        }
    }

    private fun onStorageOptionClicked(type: StorageOptionType) {
        val selectedOption = storageOptions?.find { type == it.type } ?: return
        this.selectedOption = selectedOption

        when (selectedOption.type) {
            PRIVATE_STORAGE -> onPrivateStorageSelected(selectedOption.root)
            EXTERNAL_STORAGE -> onExternalStorageSelected(selectedOption.root)
            DROPBOX, WEBDAV -> onRemoteFileStorageSelected(selectedOption.root)
        }
    }

    private fun onPrivateStorageSelected(root: FileDescriptor) {
        val action = requestedAction ?: return

        if (action == Action.PICK_FILE) {
            showFilePickerScreenEvent.call(
                FilePickerArgs(
                    action.toFilePickerAction(),
                    root,
                    false
                )
            )

        } else if (action == Action.PICK_STORAGE) {
            selectFileEvent.call(root)
        }
    }

    private fun onExternalStorageSelected(root: FileDescriptor) {
        val action = requestedAction ?: return

        showFilePickerScreenEvent.call(
            FilePickerArgs(
                action.toFilePickerAction(),
                root,
                true
            )
        )
    }

    private fun onRemoteFileStorageSelected(root: FileDescriptor) {
        val provider = fileSystemResolver.resolveProvider(root.fsAuthority)

        screenState.value = ScreenState.loading()

        val authenticator = provider.authenticator
        if (authenticator.isAuthenticationRequired()) {
            val authType = authenticator.getAuthType()

            if (authType == AuthType.EXTERNAL) {
                isExternalAuthActivityLaunched = true
            }
            showAuthActivityEvent.call(root.fsAuthority)
        } else {
            viewModelScope.launch {
                val remoteFsRoot = interactor.getRemoteFileSystemRoot(root.fsAuthority)

                onRemoteRootLoaded(remoteFsRoot)
            }
        }
    }

    private fun onRemoteRootLoaded(result: OperationResult<FileDescriptor>) {
        val action = requestedAction ?: return

        if (result.isSucceededOrDeferred) {
            val rootFile = result.obj
            showFilePickerScreenEvent.call(
                FilePickerArgs(
                    action.toFilePickerAction(),
                    rootFile,
                    true
                )
            )

        } else {
            val message = errorInteractor.processAndGetMessage(result.error)
            screenState.value = ScreenState.error(message)
        }
    }
}