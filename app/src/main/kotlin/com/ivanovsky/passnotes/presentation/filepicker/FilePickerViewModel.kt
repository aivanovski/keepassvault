package com.ivanovsky.passnotes.presentation.filepicker

import android.Manifest
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import com.ivanovsky.passnotes.presentation.core_mvvm.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core_mvvm.ScreenState
import com.ivanovsky.passnotes.presentation.core_mvvm.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core_mvvm.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core_mvvm.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core_mvvm.model.FileCellModel
import com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels.FileCellViewModel
import com.ivanovsky.passnotes.presentation.filepicker.converter.toCellModels
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilePickerViewModel(
    private val interactor: FilePickerInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resources: ResourceProvider,
    private val dateFormatProvider: DateFormatProvider,
    private val dispatchers: DispatcherProvider
) : BaseScreenViewModel() {

    val viewTypes = ViewModelTypes()
        .add(FileCellViewModel::class, R.layout.cell_file)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData<ScreenState>(ScreenState.notInitialized())
    val doneButtonVisibility = MutableLiveData<Boolean>()
    val requestPermissionEvent = SingleLiveEvent<String>()
    val selectFileAndFinishEvent = SingleLiveEvent<FileDescriptor>()
    val showSnackbarMessageEvent = SingleLiveEvent<String>()

    private lateinit var action: Action
    private lateinit var rootFile: FileDescriptor
    private var isBrowsingEnabled = false
    private var isPermissionRejected = false
    private var filePathToFileMap: Map<String, FileDescriptor> = emptyMap()
    private var selectedFile: FileDescriptor? = null
    private var currentDir: FileDescriptor? = null
    private var currentModels: List<BaseCellModel>? = null
    private val factory = FilePickerCellFactory()

    init {
        eventProvider.subscribe(this) { event ->
            if (event.containsKey(FileCellViewModel.ITEM_CLICKED_EVENT)) {
                val filePath = event.getString(FileCellViewModel.ITEM_CLICKED_EVENT)
                if (filePath != null) {
                    onItemClicked(filePath)
                }
            }
        }
    }

    fun start(
        action: Action,
        rootFile: FileDescriptor,
        isBrowsingEnabled: Boolean
    ) {
        this.action = action
        this.rootFile = rootFile
        this.isBrowsingEnabled = isBrowsingEnabled
        this.currentDir = rootFile

        loadData()
    }

    fun onDoneButtonClicked() {
        if (action == Action.PICK_DIRECTORY) {
            val currentDir = currentDir ?: return

            selectFileAndFinishEvent.call(currentDir)
        } else if (action == Action.PICK_FILE) {
            if (isAnyFileSelected()) {
                val selectedFile = selectedFile ?: return

                selectFileAndFinishEvent.call(selectedFile)
            } else {
                showSnackbarMessageEvent.call(resources.getString(R.string.please_select_any_file))
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            loadData()
        } else {
            //TODO: somehow user should see retry button
            isPermissionRejected = true
            screenState.value = ScreenState.error(
                resources.getString(R.string.permission_denied_message)
            )
            doneButtonVisibility.value = false
        }
    }

    private fun loadData() {
        screenState.value = ScreenState.loading()
        doneButtonVisibility.value = false

        viewModelScope.launch {
            val dir = currentDir ?: rootFile

            val permissionRequiredResult = interactor.isStoragePermissionRequired(dir)
            val isPermissionRequired = permissionRequiredResult.resultOrFalse
            if (!isPermissionRequired) {
                val files = withContext(dispatchers.Default) {
                    interactor.getFileList(dir)
                }

                onFilesLoaded(dir, files)
            } else {
                requestPermissionEvent.call(SDCARD_PERMISSION)
            }
        }
    }

    private fun onFilesLoaded(
        dir: FileDescriptor,
        result: OperationResult<List<FileDescriptor>>
    ) {
        val currentDir = currentDir ?: rootFile

        if (result.isSucceededOrDeferred) {
            val unsortedFiles = result.obj

            if (!dir.isRoot && isBrowsingEnabled) {
                viewModelScope.launch {
                    val parent = withContext(dispatchers.Default) {
                        interactor.getParent(currentDir)
                    }

                    onParentLoaded(unsortedFiles, parent)
                }

            } else {
                val sortedFiles = sortFiles(unsortedFiles)

                val displayedFiles = if (isBrowsingEnabled) {
                    sortedFiles
                } else {
                    // hide all directories
                    sortedFiles.filter { file -> !file.isDirectory }
                }

                val cellModels = displayedFiles.toCellModels(
                    dir,
                    dateFormatProvider.getShortDateFormat()
                )

                filePathToFileMap = createFileMap(displayedFiles)

                if (displayedFiles.isNotEmpty()) {
                    setCellElements(factory.createCellViewModels(cellModels, eventProvider))
                    currentModels = cellModels
                    screenState.value = ScreenState.data()
                    doneButtonVisibility.value = true
                } else {
                    screenState.value = ScreenState.empty(
                        resources.getString(R.string.no_items)
                    )
                }
            }
        } else {
            val message = errorInteractor.processAndGetMessage(result.error)
            screenState.value = ScreenState.error(message)
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

            val cellModels = sortedFiles.toCellModels(
                parent,
                dateFormatProvider.getShortDateFormat()
            )

            filePathToFileMap = createFileMap(sortedFiles)

            setCellElements(factory.createCellViewModels(cellModels, eventProvider))
            currentModels = cellModels
            screenState.value = ScreenState.data()
            doneButtonVisibility.value = true
        } else {
            val message = errorInteractor.processAndGetMessage(result.error)
            screenState.value = ScreenState.error(message)
            doneButtonVisibility.value = false
        }
    }

    private fun onItemClicked(filePath: String) {
        val selectedFile = filePathToFileMap[filePath] ?: return

        if (selectedFile.isDirectory) {
            currentDir = selectedFile

            loadData()
        } else if (action == Action.PICK_FILE) {
            val models = currentModels ?: return

            val selectedModel = (models.find { model -> model.id == filePath } as? FileCellModel)
                ?: return

            val newModels = if (selectedModel.isSelected) {
                val modelIdx = models.indexOf(selectedModel)
                val newModels = models.toMutableList()
                newModels[modelIdx] = selectedModel.copy(isSelected = false)
                newModels
            } else {
                val newModels = models.map { model ->
                    if (selectedModel == model) {
                        selectedModel.copy(isSelected = true)
                    } else if (model is FileCellModel && model.isSelected) {
                        model.copy(isSelected = false)
                    } else {
                        model
                    }
                }

                newModels
            }
            setCellElements(factory.createCellViewModels(newModels, eventProvider))
            currentModels = newModels
            this.selectedFile = selectedFile
        }
    }

    private fun sortFiles(files: List<FileDescriptor>): List<FileDescriptor> {
        return files.sortedWith(Comparator { lhs, rhs ->
            if ((lhs.isDirectory && !rhs.isDirectory) || (!lhs.isDirectory && rhs.isDirectory)) {
                if (lhs.isDirectory) -1 else 1
            } else {//if files have same type
                lhs.name.compareTo(rhs.name)
            }
        })
    }

    private fun createFileMap(files: List<FileDescriptor>): Map<String, FileDescriptor> {
        return files.map { file -> Pair(file.path, file) }
            .toMap()
    }

    private fun isAnyFileSelected(): Boolean = (selectedFile != null)

    companion object {
        private const val SDCARD_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
}