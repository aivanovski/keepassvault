package com.ivanovsky.passnotes.presentation.storagelist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.domain.entity.StorageOptionType
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.*
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor
import com.ivanovsky.passnotes.presentation.core_mvvm.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core_mvvm.ScreenState
import com.ivanovsky.passnotes.presentation.core_mvvm.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core_mvvm.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels.SingleTextCellViewModel
import com.ivanovsky.passnotes.presentation.storagelist.model.FilePickerArgs
import com.ivanovsky.passnotes.presentation.storagelist.converter.toCellModels
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
    val showAuthActivityEvent = SingleLiveEvent<FSType>()
    val showFilePickerScreenEvent = SingleLiveEvent<FilePickerArgs>()

    private val cellFactory = StorageListCellFactory()
    private var storageOptions: List<StorageOption>? = null
    private var requestedAction: Action? = null
    private var isAuthActivityDisplayed = false

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
        if (isAuthActivityDisplayed) {
            isAuthActivityDisplayed = false

            val provider = fileSystemResolver.resolveProvider(FSType.DROPBOX)
            if (provider.authenticator.isAuthenticationRequired) {
                val errorMessage = resourceProvider.getString(R.string.authentication_failed)
                screenState.value = ScreenState.dataWithError(errorMessage)
            } else {
                viewModelScope.launch {
                    val dropboxRoot = withContext(dispatchers.IO) {
                        interactor.getDropboxRoot()
                    }

                    onDropboxRootLoaded(dropboxRoot)
                }
            }
        }
    }

    fun onFilePickedByPicker(file: FileDescriptor) {
        selectFileEvent.call(file)
    }

    private fun subscribeToEvents() {
        eventProvider.subscribe(this) { event ->
            if (event.containsKey(SingleTextCellViewModel.CLICKED_ITEM_ID)) {
                val id = event.getString(SingleTextCellViewModel.CLICKED_ITEM_ID) ?: ""
                onStorageOptionClicked(valueOf(id))
            }
        }
    }

    private fun onStorageOptionClicked(type: StorageOptionType) {
        val selectedOption = storageOptions?.find { type == it.type } ?: return

        when (selectedOption.type) {
            PRIVATE_STORAGE -> onPrivateStorageSelected(selectedOption.root)
            EXTERNAL_STORAGE -> onExternalStorageSelected(selectedOption.root)
            DROPBOX -> onDropboxStorageSelected()
        }
    }

    private fun onPrivateStorageSelected(root: FileDescriptor) {
        val action = requestedAction ?: return

        if (action == Action.PICK_FILE) {
            showFilePickerScreenEvent.call(
                FilePickerArgs(
                    root,
                    action,
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
                root,
                action,
                true
            )
        )
    }

    private fun onDropboxStorageSelected() {
        val provider = fileSystemResolver.resolveProvider(FSType.DROPBOX)

        screenState.value = ScreenState.loading()

        if (provider.authenticator.isAuthenticationRequired) {
            isAuthActivityDisplayed = true
            showAuthActivityEvent.call(FSType.DROPBOX)

        } else {
            viewModelScope.launch {
                val dropboxRoot = withContext(dispatchers.IO) {
                    interactor.getDropboxRoot()
                }

                onDropboxRootLoaded(dropboxRoot)
            }
        }
    }

    private fun onDropboxRootLoaded(result: OperationResult<FileDescriptor>) {
        val action = requestedAction ?: return

        if (result.isSucceededOrDeferred) {
            val rootFile = result.obj
            showFilePickerScreenEvent.call(
                FilePickerArgs(
                    rootFile,
                    action,
                    true
                )
            )

        } else {
            val message = errorInteractor.processAndGetMessage(result.error)
            screenState.value = ScreenState.error(message)
        }
    }
}