package com.ivanovsky.passnotes.presentation.storagelist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FSType.EXTERNAL_STORAGE
import com.ivanovsky.passnotes.data.entity.FSType.FAKE
import com.ivanovsky.passnotes.data.entity.FSType.GIT
import com.ivanovsky.passnotes.data.entity.FSType.INTERNAL_STORAGE
import com.ivanovsky.passnotes.data.entity.FSType.SAF
import com.ivanovsky.passnotes.data.entity.FSType.UNDEFINED
import com.ivanovsky.passnotes.data.entity.FSType.WEBDAV
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.Screens.FilePickerScreen
import com.ivanovsky.passnotes.presentation.Screens.ServerLoginScreen
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenVisibilityHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.viewmodel.OneLineTextCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.TwoTextWithIconCellViewModel
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerArgs
import com.ivanovsky.passnotes.presentation.serverLogin.ServerLoginArgs
import com.ivanovsky.passnotes.presentation.serverLogin.model.LoginType
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
        .add(OneLineTextCellViewModel::class, R.layout.cell_single_text)
        .add(TwoTextWithIconCellViewModel::class, R.layout.cell_two_text_with_icon)

    val screenStateHandler = DefaultScreenVisibilityHandler()
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
                setErrorPanelState(OperationError.newErrorMessage(errorMessage, Stacktrace()))
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

        if (fsAuthority.type == FSType.SAF) {
            val setupPermissionResult = interactor.setupPermissionForSaf(uri)
            if (setupPermissionResult.isFailed) {
                setErrorPanelState(setupPermissionResult.error)
                return
            }
        }

        val path = uri.toString()

        viewModelScope.launch {
            val getFileResult = interactor.getFileByPath(path, fsAuthority)

            if (getFileResult.isSucceededOrDeferred) {
                val file = getFileResult.obj

                router.exit()
                router.sendResult(args.resultKey, file)
            } else {
                setErrorPanelState(getFileResult.error)
            }
        }
    }

    fun onExternalStorageFileSelectionCanceled() {
        setScreenState(ScreenState.data())
    }

    fun navigateBack() = router.exit()

    private fun loadData() {
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val getOptionsResult = interactor.getStorageOptions(args.action)
            if (getOptionsResult.isSucceededOrDeferred) {
                val options = getOptionsResult.obj
                storageOptions = options

                val cellModels = modelFactory.createCellModel(options)
                setCellViewModels(viewModelFactory.createCellViewModels(cellModels, eventProvider))

                setScreenState(ScreenState.data())
            } else {
                setErrorState(getOptionsResult.error)
            }
        }
    }

    private fun onInternalAuthFailed() {
        setScreenState(ScreenState.data())
    }

    private fun navigateToFilePicker(args: FilePickerArgs) {
        setScreenState(ScreenState.loading())
        isReloadOnStart = true
        router.setResultListener(FilePickerScreen.RESULT_KEY) { file ->
            if (file is FileDescriptor) {
                isReloadOnStart = false
                onFilePickedByPicker(file)
            }
        }
        router.navigateTo(FilePickerScreen(args))
    }

    private fun navigateToServerLogin(fsAuthority: FSAuthority) {
        val screenArgs = when (fsAuthority.type) {
            WEBDAV -> {
                ServerLoginArgs(
                    loginType = LoginType.USERNAME_PASSWORD,
                    fsAuthority = fsAuthority
                )
            }

            GIT -> {
                ServerLoginArgs(
                    loginType = LoginType.GIT,
                    fsAuthority = fsAuthority
                )
            }

            FAKE -> {
                ServerLoginArgs(
                    loginType = LoginType.USERNAME_PASSWORD,
                    fsAuthority = fsAuthority
                )
            }

            else -> throw IllegalArgumentException()
        }

        setScreenState(ScreenState.loading())
        isReloadOnStart = true
        router.setResultListener(ServerLoginScreen.RESULT_KEY) { file ->
            if (file is FileDescriptor) {
                isReloadOnStart = false
                setScreenState(ScreenState.loading())
                if (file.isDirectory) {
                    navigateToFilePicker(
                        FilePickerArgs(
                            action = args.action.toFilePickerAction(),
                            rootFile = file,
                            isBrowsingEnabled = true
                        )
                    )
                } else {
                    onFilePickedByPicker(file)
                }
            } else {
                onInternalAuthFailed()
            }
        }
        router.navigateTo(ServerLoginScreen(screenArgs))
    }

    private fun onFilePickedByPicker(file: FileDescriptor) {
        router.exit()
        router.sendResult(args.resultKey, file)
    }

    private fun subscribeToEvents() {
        eventProvider.subscribe(this) { event ->
            when {
                event.containsKey(OneLineTextCellViewModel.CLICK_EVENT) -> {
                    val id = event.getString(OneLineTextCellViewModel.CLICK_EVENT) ?: EMPTY
                    val fsType = FSType.findByValue(id) ?: throw IllegalArgumentException()
                    onStorageOptionClicked(fsType)
                }

                event.containsKey(TwoTextWithIconCellViewModel.CLICK_EVENT) -> {
                    val id = event.getString(TwoTextWithIconCellViewModel.CLICK_EVENT) ?: EMPTY
                    val fsType = FSType.findByValue(id) ?: throw IllegalArgumentException()
                    onStorageOptionClicked(fsType)
                }
            }
        }
    }

    private fun onStorageOptionClicked(fsType: FSType) {
        val selectedOption = storageOptions?.find { fsType == it.root.fsAuthority.type } ?: return
        this.selectedOption = selectedOption

        when (selectedOption.root.fsAuthority.type) {
            INTERNAL_STORAGE, EXTERNAL_STORAGE -> {
                onDeviceStorageSelected(selectedOption.root, selectedOption.root.fsAuthority.type)
            }

            SAF -> onSafStorageSelected()
            WEBDAV, GIT, FAKE -> onRemoteFileStorageSelected(selectedOption.root)
            UNDEFINED -> {}
        }
    }

    private fun onDeviceStorageSelected(root: FileDescriptor, type: FSType) {
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

                INTERNAL_STORAGE -> {
                    router.exit()
                    router.sendResult(args.resultKey, root)
                }

                else -> throw IllegalArgumentException()
            }
        }
    }

    private fun onSafStorageSelected() {
        setScreenState(ScreenState.loading())

        when (args.action) {
            Action.PICK_FILE -> {
                showSystemFilePickerEvent.call(Unit)
            }

            Action.PICK_STORAGE -> {
                showSystemFileCreatorEvent.call(Unit)
            }
        }
    }

    private fun onRemoteFileStorageSelected(root: FileDescriptor) {
        val provider = fileSystemResolver.resolveProvider(root.fsAuthority)

        setScreenState(ScreenState.loading())

        val authenticator = provider.authenticator
        if (authenticator.isAuthenticationRequired()) {
            val authType = authenticator.getAuthType()
            if (authType == AuthType.CREDENTIALS) {
                navigateToServerLogin(root.fsAuthority)
            } else if (authType == AuthType.EXTERNAL) {
                isExternalAuthActivityLaunched = true
                showAuthActivityEvent.call(root.fsAuthority)
            }
        } else {
            loadRootAndNavigateToPicker(root.fsAuthority)
        }
    }

    private fun loadRootAndNavigateToPicker(fsAuthority: FSAuthority) {
        setScreenState(ScreenState.loading())

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
                setErrorState(getRootResult.error)
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