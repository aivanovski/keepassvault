package com.ivanovsky.passnotes.presentation.filepicker

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.SystemPermission
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.Screens.FilePickerScreen
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.FileCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.FileCellViewModel
import com.ivanovsky.passnotes.presentation.filepicker.factory.FilePickerCellModelFactory
import com.ivanovsky.passnotes.presentation.filepicker.factory.FilePickerCellViewModelFactory
import com.ivanovsky.passnotes.util.FileUtils
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class FilePickerViewModel(
    private val interactor: FilePickerInteractor,
    private val errorInteractor: ErrorInteractor,
    private val fileSystemResolver: FileSystemResolver,
    private val modelFactory: FilePickerCellModelFactory,
    private val viewModelFactory: FilePickerCellViewModelFactory,
    private val resourceProvider: ResourceProvider,
    private val dateFormatProvider: DateFormatProvider,
    private val permissionHelper: PermissionHelper,
    private val router: Router,
    private val args: FilePickerArgs
) : BaseScreenViewModel() {

    val viewTypes = ViewModelTypes()
        .add(FileCellViewModel::class, R.layout.cell_file)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.notInitialized())
    val isDoneButtonVisible = MutableLiveData<Boolean>()
    val requestPermissionEvent = SingleLiveEvent<SystemPermission>()
    val showAllFilePermissionDialogEvent = SingleLiveEvent<Unit>()
    val showSnackbarMessageEvent = SingleLiveEvent<String>()
    val currentPath = MutableLiveData(EMPTY)

    private var isPermissionRejected = false
    private var filePathToFileMap: Map<String, FileDescriptor> = emptyMap()
    private var selectedFile: FileDescriptor? = null
    private var currentDir: FileDescriptor? = null
    private var currentModels: List<BaseCellModel>? = null

    init {
        eventProvider.subscribe(this) { event ->
            if (event.containsKey(FileCellViewModel.CLICK_EVENT)) {
                val filePath = event.getString(FileCellViewModel.CLICK_EVENT)
                if (filePath != null) {
                    onItemClicked(filePath)
                }
            }
        }
    }

    fun start() {
        if (currentDir == null) {
            currentDir = args.rootFile
        }

        val authenticator = fileSystemResolver
            .resolveProvider(args.rootFile.fsAuthority)
            .authenticator
        val authType = authenticator.getAuthType()

        if (authenticator.isAuthenticationRequired() &&
            authType != AuthType.SDCARD_PERMISSION &&
            authType != AuthType.ALL_FILES_PERMISSION
        ) {
            setScreenState(
                ScreenState.error(
                    errorText = resourceProvider.getString(R.string.unable_to_authenticate)
                )
            )
            return
        }

        if (isPermissionWasRejectedAndGrantedFromBackground()) {
            isPermissionRejected = false
            loadData()
            return
        } else if (isPermissionRejectedFromBackground() || isPermissionRejected) {
            return
        }

        if (authenticator.isAuthenticationRequired()) {
            setScreenState(
                ScreenState.error(
                    resourceProvider.getString(R.string.permission_is_required)
                )
            )

            permissionHelper.getRequiredFilePermission()?.let { permission ->
                if (permission == SystemPermission.ALL_FILES_PERMISSION) {
                    showAllFilePermissionDialogEvent.call(Unit)
                } else {
                    requestPermissionEvent.call(permission)
                }
            }
        } else {
            loadData()
        }
    }

    fun onDoneButtonClicked() {
        if (args.action == Action.PICK_DIRECTORY) {
            val currentDir = currentDir ?: return

            router.exit()
            router.sendResult(FilePickerScreen.RESULT_KEY, currentDir)
        } else if (args.action == Action.PICK_FILE) {
            if (isAnyFileSelected()) {
                val selectedFile = selectedFile ?: return

                router.exit()
                router.sendResult(FilePickerScreen.RESULT_KEY, selectedFile)
            } else {
                showSnackbarMessageEvent.call(
                    resourceProvider.getString(R.string.please_select_any_file)
                )
            }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        isPermissionRejected = !isGranted

        if (isGranted) {
            loadData()
        } else {
            setScreenState(
                ScreenState.error(
                    resourceProvider.getString(R.string.permission_is_required)
                )
            )
        }
    }

    fun onBackClicked() {
        val currentDir = currentDir ?: return

        if (currentDir.isRoot || !args.isBrowsingEnabled) {
            navigateToPreviousScreen()
        } else {
            val parentPath = FileUtils.getParentPath(currentDir.path)
            if (parentPath != null && filePathToFileMap.containsKey(parentPath)) {
                onItemClicked(parentPath)
            } else {
                navigateToPreviousScreen()
            }
        }
    }

    fun navigateToPreviousScreen() = router.exit()

    private fun loadData() {
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val dir = currentDir ?: args.rootFile
            val files = interactor.getFileList(dir)
            onFilesLoaded(dir, files)
        }
    }

    private fun onFilesLoaded(
        dir: FileDescriptor,
        result: OperationResult<List<FileDescriptor>>
    ) {
        val currentDir = currentDir ?: args.rootFile

        if (result.isSucceededOrDeferred) {
            val unsortedFiles = result.obj

            currentPath.value = currentDir.path

            if (!dir.isRoot && args.isBrowsingEnabled) {
                viewModelScope.launch {
                    val parent = interactor.getParent(currentDir)
                    onParentLoaded(unsortedFiles, parent)
                }
            } else {
                val sortedFiles = sortFiles(unsortedFiles)

                val displayedFiles = if (args.isBrowsingEnabled) {
                    sortedFiles
                } else {
                    // hide all directories
                    sortedFiles.filter { file -> !file.isDirectory }
                }

                val cellModels = modelFactory.createCellModels(
                    files = displayedFiles,
                    parentDir = dir,
                    dateFormat = dateFormatProvider.getShortDateFormat()
                )

                filePathToFileMap = createFileMap(displayedFiles)

                if (displayedFiles.isNotEmpty()) {
                    setCellElements(
                        viewModelFactory.createCellViewModels(
                            cellModels,
                            eventProvider
                        )
                    )
                    currentModels = cellModels
                    setScreenState(ScreenState.data())
                } else {
                    setScreenState(
                        ScreenState.empty(
                            resourceProvider.getString(R.string.no_items)
                        )
                    )
                }
            }
        } else {
            setScreenState(
                ScreenState.error(
                    errorText = errorInteractor.processAndGetMessage(result.error)
                )
            )
        }
    }

    private fun onParentLoaded(
        unsortedFiles: List<FileDescriptor>,
        result: OperationResult<FileDescriptor>
    ) {
        if (result.isSucceededOrDeferred) {
            val parent = result.obj

            val sortedFiles = sortFiles(unsortedFiles).toMutableList()
            sortedFiles.add(0, parent)

            val cellModels = modelFactory.createCellModels(
                files = sortedFiles,
                parentDir = parent,
                dateFormat = dateFormatProvider.getShortDateFormat()
            )

            filePathToFileMap = createFileMap(sortedFiles)

            setCellElements(viewModelFactory.createCellViewModels(cellModels, eventProvider))
            currentModels = cellModels
            setScreenState(ScreenState.data())
        } else {
            setScreenState(
                ScreenState.error(
                    errorText = errorInteractor.processAndGetMessage(result.error)
                )
            )
        }
    }

    private fun onItemClicked(filePath: String) {
        val selectedFile = filePathToFileMap[filePath] ?: return

        if (selectedFile.isDirectory) {
            currentDir = selectedFile

            loadData()
        } else if (args.action == Action.PICK_FILE) {
            val models = currentModels ?: return

            val selectedModel = (models.find { model -> model.id == filePath } as? FileCellModel)
                ?: return

            val newModels = if (selectedModel.isSelected) {
                val modelIdx = models.indexOf(selectedModel)
                models.toMutableList()
                    .apply {
                        this[modelIdx] = selectedModel.copy(isSelected = false)
                    }
            } else {
                models.map { model ->
                    if (selectedModel == model) {
                        selectedModel.copy(isSelected = true)
                    } else if (model is FileCellModel && model.isSelected) {
                        model.copy(isSelected = false)
                    } else {
                        model
                    }
                }
            }
            setCellElements(viewModelFactory.createCellViewModels(newModels, eventProvider))
            currentModels = newModels
            this.selectedFile = selectedFile
        }
    }

    private fun sortFiles(files: List<FileDescriptor>): List<FileDescriptor> {
        return files.sortedWith { lhs, rhs ->
            if ((lhs.isDirectory && !rhs.isDirectory) || (!lhs.isDirectory && rhs.isDirectory)) {
                if (lhs.isDirectory) -1 else 1
            } else { // if files have same type
                lhs.name.compareTo(rhs.name)
            }
        }
    }

    private fun setScreenState(state: ScreenState) {
        screenState.value = state
        isDoneButtonVisible.value = getDoneButtonVisibility()
    }

    private fun getDoneButtonVisibility(): Boolean {
        val screenState = this.screenState.value ?: return false

        return screenState.isDisplayingData
    }

    private fun createFileMap(files: List<FileDescriptor>): Map<String, FileDescriptor> {
        return files.associateBy { file -> file.path }
    }

    private fun isAnyFileSelected(): Boolean = (selectedFile != null)

    private fun isPermissionWasRejectedAndGrantedFromBackground(): Boolean {
        val currentScreenState = screenState.value ?: return false

        return isPermissionRejected &&
            permissionHelper.hasFileAccessPermission() &&
            !currentScreenState.isDisplayingData
    }

    private fun isPermissionRejectedFromBackground(): Boolean {
        val currentScreenState = screenState.value ?: return false

        return !permissionHelper.hasFileAccessPermission() &&
            (currentScreenState.isDisplayingData || currentScreenState.isDisplayingEmptyState)
    }

    private fun shouldReloadData(): Boolean {
        val currentScreenState = screenState.value ?: return false

        return !currentScreenState.isDisplayingData && !currentScreenState.isDisplayingLoading
    }

    class Factory(private val args: FilePickerArgs) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<FilePickerViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}