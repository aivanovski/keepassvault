package com.ivanovsky.passnotes.presentation.storagelist

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.domain.entity.StorageOptionType
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.DROPBOX
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.EXTERNAL_STORAGE
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.SAF_STORAGE
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.PRIVATE_STORAGE
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.WEBDAV
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.Screens.FilePickerScreen
import com.ivanovsky.passnotes.presentation.Screens.ServerLoginScreen
import com.ivanovsky.passnotes.presentation.Screens.StorageListScreen
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.viewmodel.SingleTextCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.TwoTextWithIconCellViewModel
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerArgs
import com.ivanovsky.passnotes.presentation.server_login.ServerLoginArgs
import com.ivanovsky.passnotes.presentation.storagelist.factory.StorageListCellModelFactory
import com.ivanovsky.passnotes.presentation.storagelist.factory.StorageListCellViewModelFactory
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class StorageListViewModel(
    private val interactor: StorageListInteractor,
    private val errorInteractor: ErrorInteractor,
    private val modelFactory: StorageListCellModelFactory,
    private val viewModelFactory: StorageListCellViewModelFactory,
    private val fileSystemResolver: FileSystemResolver,
    private val resourceProvider: ResourceProvider,
    private val router: Router,
    private val args: StorageListArgs
) : BaseScreenViewModel() {

    val viewTypes = ViewModelTypes()
        .add(SingleTextCellViewModel::class, R.layout.cell_single_text)
        .add(TwoTextWithIconCellViewModel::class, R.layout.cell_two_text_with_icon)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.notInitialized())
    val showAuthActivityEvent = SingleLiveEvent<FSAuthority>()
    val showSystemFilePickerEvent = SingleLiveEvent<Unit>()
    val showSystemFileCreatorEvent = SingleLiveEvent<Unit>()

    private var storageOptions: List<StorageOption>? = null
    private var isExternalAuthActivityLaunched = false
    private var isReloadOnStart = false
    private var selectedOption: StorageOption? = null

    init {
        subscribeToEvents()
    }

    fun onScreenStart() {
        val currentScreenState = screenState.value ?: return
        val selectedOption = selectedOption

        if (currentScreenState.isNotInitialized) {
            loadData()
        }

        if (isExternalAuthActivityLaunched && selectedOption != null) {
            isExternalAuthActivityLaunched = false

            val fsAuthority = selectedOption.root.fsAuthority
            val provider = fileSystemResolver.resolveProvider(fsAuthority)
            if (provider.authenticator.isAuthenticationRequired()) {
                val errorMessage = resourceProvider.getString(R.string.authentication_failed)
                screenState.value = ScreenState.dataWithError(errorMessage)
            } else {
                loadRootAndNavigateToPicker(fsAuthority)
            }
        } else if (isReloadOnStart) {
            isReloadOnStart = false
            loadData()
        }
    }

    fun onExternalStorageFileSelected(uri: Uri) {
        val fsAuthority = selectedOption?.root?.fsAuthority ?: return

        val path = uri.toString()

        viewModelScope.launch {
            val getFileResult = interactor.getFileByPath(path, fsAuthority)

            if (getFileResult.isSucceededOrDeferred) {
                val file = getFileResult.obj
                router.sendResult(StorageListScreen.RESULT_KEY, file)
                router.exit()
            } else {
                val message = errorInteractor.processAndGetMessage(getFileResult.error)
                screenState.value = ScreenState.dataWithError(message)
            }
        }
    }

    fun onExternalStorageFileSelectionCanceled() {
        screenState.value = ScreenState.data()
    }

    fun navigateBack() = router.exit()

    private fun loadData() {
        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val options = interactor.getStorageOptions(args.action)

            val cellModels = modelFactory.createCellModel(options)
            setCellElements(viewModelFactory.createCellViewModels(cellModels, eventProvider))

            storageOptions = options
            screenState.value = ScreenState.data()
        }
    }

    private fun onInternalAuthSuccess(fsAuthority: FSAuthority) {
        loadRootAndNavigateToPicker(fsAuthority)
    }

    private fun onInternalAuthFailed() {
        screenState.value = ScreenState.data()
    }

    private fun navigateToFilePicker(args: FilePickerArgs) {
        screenState.value = ScreenState.loading()
        isReloadOnStart = true
        router.setResultListener(FilePickerScreen.RESULT_KEY) { file ->
            if (file is FileDescriptor) {
                isReloadOnStart = false
                onFilePickedByPicker(file)
            }
        }
        router.navigateTo(FilePickerScreen(args))
    }

    private fun navigateToServerLogin(args: ServerLoginArgs) {
        screenState.value = ScreenState.loading()
        isReloadOnStart = true
        router.setResultListener(ServerLoginScreen.RESULT_KEY) { fsAuthority ->
            if (fsAuthority is FSAuthority) {
                isReloadOnStart = false
                onInternalAuthSuccess(fsAuthority)
            } else {
                onInternalAuthFailed()
            }
        }
        router.navigateTo(ServerLoginScreen(args))
    }

    private fun onFilePickedByPicker(file: FileDescriptor) {
        router.sendResult(StorageListScreen.RESULT_KEY, file)
        router.exit()
    }

    private fun subscribeToEvents() {
        eventProvider.subscribe(this) { event ->
            when {
                event.containsKey(SingleTextCellViewModel.CLICK_EVENT) -> {
                    val id = event.getString(SingleTextCellViewModel.CLICK_EVENT) ?: EMPTY
                    onStorageOptionClicked(StorageOptionType.valueOf(id))
                }
                event.containsKey(TwoTextWithIconCellViewModel.CLICK_EVENT) -> {
                    val id = event.getString(TwoTextWithIconCellViewModel.CLICK_EVENT) ?: EMPTY
                    onStorageOptionClicked(StorageOptionType.valueOf(id))
                }
            }
        }
    }

    private fun onStorageOptionClicked(type: StorageOptionType) {
        val selectedOption = storageOptions?.find { type == it.type } ?: return
        this.selectedOption = selectedOption

        when (selectedOption.type) {
            PRIVATE_STORAGE, EXTERNAL_STORAGE -> {
                onDeviceStorageSelected(selectedOption.root, selectedOption.type)
            }
            SAF_STORAGE -> onSafStorageSelected()
            DROPBOX, WEBDAV -> onRemoteFileStorageSelected(selectedOption.root)
        }
    }

    private fun onDeviceStorageSelected(root: FileDescriptor, type: StorageOptionType) {
        if (args.action == Action.PICK_FILE) {
            navigateToFilePicker(
                FilePickerArgs(
                    action = args.action.toFilePickerAction(),
                    rootFile = root,
                    isBrowsingEnabled = (type == EXTERNAL_STORAGE)
                )
            )
        } else if (args.action == Action.PICK_STORAGE) {
            when (type) {
                EXTERNAL_STORAGE -> {
                    loadRootAndNavigateToPicker(root.fsAuthority)
                }
                PRIVATE_STORAGE -> {
                    router.sendResult(StorageListScreen.RESULT_KEY, root)
                    router.exit()
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    private fun onSafStorageSelected() {
        screenState.value = ScreenState.loading()

        when (args.action) {
            Action.PICK_FILE -> {
                showSystemFilePickerEvent.call()
            }
            Action.PICK_STORAGE -> {
                showSystemFileCreatorEvent.call()
            }
        }
    }

    private fun onRemoteFileStorageSelected(root: FileDescriptor) {
        val provider = fileSystemResolver.resolveProvider(root.fsAuthority)

        screenState.value = ScreenState.loading()

        val authenticator = provider.authenticator
        if (authenticator.isAuthenticationRequired()) {
            val authType = authenticator.getAuthType()
            if (authType == AuthType.CREDENTIALS) {
                navigateToServerLogin(ServerLoginArgs(root.fsAuthority))
            } else if (authType == AuthType.EXTERNAL) {
                isExternalAuthActivityLaunched = true
                showAuthActivityEvent.call(root.fsAuthority)
            }
        } else {
            loadRootAndNavigateToPicker(root.fsAuthority)
        }
    }

    private fun loadRootAndNavigateToPicker(fsAuthority: FSAuthority) {
        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val getRootResult = interactor.getFileSystemRoot(fsAuthority)
            if (getRootResult.isSucceededOrDeferred) {
                val rootFile = getRootResult.obj
                navigateToFilePicker(
                    FilePickerArgs(
                        action = args.action.toFilePickerAction(),
                        rootFile = rootFile,
                        isBrowsingEnabled = true
                    )
                )
            } else {
                val message = errorInteractor.processAndGetMessage(getRootResult.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    class Factory(private val args: StorageListArgs) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<StorageListViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}