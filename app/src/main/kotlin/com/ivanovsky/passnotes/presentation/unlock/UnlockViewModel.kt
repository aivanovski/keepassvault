package com.ivanovsky.passnotes.presentation.unlock

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.unlock.model.DropDownItem
import com.ivanovsky.passnotes.presentation.unlock.model.PasswordRule
import com.ivanovsky.passnotes.util.FileUtils
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList
import java.util.regex.Pattern

class UnlockViewModel(
    private val interactor: UnlockInteractor,
    private val errorInteractor: ErrorInteractor,
    private val observerBus: ObserverBus,
    private val resourceProvider: ResourceProvider,
    private val dispatchers: DispatcherProvider
) : ViewModel(),
    ObserverBus.UsedFileDataSetObserver,
    ObserverBus.UsedFileContentObserver{

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData<ScreenState>(ScreenState.notInitialized())
    val items = MutableLiveData<List<DropDownItem>>()
    val selectedItem = MutableLiveData<Int>()
    val password = MutableLiveData<String>(EMPTY)
    val showGroupsScreenEvent = SingleLiveEvent<Unit>()
    val showNewDatabaseScreenEvent = SingleLiveEvent<Unit>()
    val showOpenFileScreenEvent = SingleLiveEvent<Unit>()
    val showSettingsScreenEvent = SingleLiveEvent<Unit>()
    val showAboutScreenEvent = SingleLiveEvent<Unit>()
    val showDebugMenuScreenEvent = SingleLiveEvent<Unit>()
    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val showSnackbarMessage = SingleLiveEvent<String>()
    val debugPasswordRules: List<PasswordRule>

    private var selectedRecentlyUsedFile: FileDescriptor? = null
    private var selectedPosition: Int? = null
    private var recentlyUsedFiles: List<FileDescriptor>? = null

    init {
        screenState.value = ScreenState.loading()
        observerBus.register(this)

        debugPasswordRules = if (BuildConfig.DEBUG) {
            createDebugPasswordRulesForAutoFill()
        } else {
            emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        observerBus.unregister(this)
    }

    override fun onUsedFileDataSetChanged() {
        loadData(resetSelection = true)
    }

    override fun onUsedFileContentChanged(usedFileId: Int) {
        loadData(resetSelection = false)
    }

    fun onScreenStart() {
        closeActiveDatabaseIfNeed()
    }

    fun loadData(resetSelection: Boolean) {
        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val result = withContext(dispatchers.IO) {
                interactor.getRecentlyOpenedFiles()
            }

            if (result.isSucceededOrDeferred) {
                val files = result.obj
                if (files.isNotEmpty()) {
                    recentlyUsedFiles = files

                    items.value = createViewItems(files)

                    if (resetSelection) {
                        selectedPosition = null
                        selectedRecentlyUsedFile = null
                    }

                    selectAlreadySelectedOrFirstFile(files)

                    screenState.value = ScreenState.data()
                } else {
                    val emptyText = resourceProvider.getString(R.string.no_files_to_open)
                    screenState.value = ScreenState.empty(emptyText)
                }
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    private fun closeActiveDatabaseIfNeed() {
        if (interactor.hasActiveDatabase()) {
            viewModelScope.launch {
                val closeResult = withContext(dispatchers.IO) {
                    interactor.closeActiveDatabase()
                }

                if (closeResult.isFailed) {
                    val message = errorInteractor.processAndGetMessage(closeResult.error)
                    screenState.value = ScreenState.error(message)
                }
            }
        }
    }

    private fun selectAlreadySelectedOrFirstFile(files: List<FileDescriptor>) {
        val selectedFile = selectedRecentlyUsedFile
        if (selectedFile == null) {
            selectedRecentlyUsedFile = files[0]
            selectedPosition = 0
            selectedItem.value = 0
        } else {
            val newSelectedPosition = indexOfFile(files, selectedFile)
            if (newSelectedPosition >= 0 && newSelectedPosition != selectedPosition) {
                selectedPosition = newSelectedPosition
                selectedItem.value = newSelectedPosition
            } else if (newSelectedPosition == -1) {
                selectedRecentlyUsedFile = files[0]
                selectedPosition = 0
                selectedItem.value = 0
            }
        }
    }

    private fun createViewItems(files: List<FileDescriptor>): List<DropDownItem> {
        return files.map { file ->
            DropDownItem(
                FileUtils.getFileNameFromPath(file.path)
                    ?: resourceProvider.getString(R.string.empty_file_name),
                file.path,
                formatFsType(file.fsType)
            )
        }
    }

    private fun formatFsType(fsType: FSType): String {
        return when (fsType) {
            FSType.DROPBOX -> resourceProvider.getString(R.string.dropbox)
            FSType.REGULAR_FS -> resourceProvider.getString(R.string.device)
        }
    }

    private fun indexOfFile(files: List<FileDescriptor>, fileToFind: FileDescriptor): Int {
        return files.indexOfFirst { file -> isFileEqualsByUidAndFsType(file, fileToFind)}
    }

    private fun isFileEqualsByUidAndFsType(lhs: FileDescriptor, rhs: FileDescriptor): Boolean {
        return lhs.uid == rhs.uid && lhs.fsType == rhs.fsType
    }

    fun onUnlockButtonClicked() {
        val password = this.password.value ?: return
        val selectedFile = selectedRecentlyUsedFile ?: return

        hideKeyboardEvent.call()
        screenState.value = ScreenState.loading()

        val key = KeepassDatabaseKey(password)

        viewModelScope.launch {
            val result = withContext(dispatchers.IO) {
                interactor.openDatabase(key, selectedFile)
            }

            if (result.isSucceededOrDeferred) {
                showGroupsScreenEvent.call()
                screenState.value = ScreenState.data()
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                screenState.value = ScreenState.dataWithError(message)
            }
        }
    }

    fun onOpenFileMenuClicked() {
        showOpenFileScreenEvent.call()
    }

    fun onSettingsMenuClicked() {
        showSettingsScreenEvent.call()
    }

    fun onAboutMenuClicked() {
        showAboutScreenEvent.call()
    }

    fun onDebugMenuClicked() {
        showDebugMenuScreenEvent.call()
    }

    fun onFilePicked(file: FileDescriptor) {
        //called when user select file from built-in file picker
        screenState.value = ScreenState.loading()

        val usedFile = UsedFile()

        usedFile.filePath = file.path
        usedFile.fileUid = file.uid
        usedFile.fsType = file.fsType
        usedFile.addedTime = System.currentTimeMillis()

        viewModelScope.launch {
            val result = withContext(dispatchers.Default) {
                interactor.saveUsedFileWithoutAccessTime(usedFile)
            }

            if (result.isSucceededOrDeferred) {
                loadData(resetSelection = false)

            } else {
                screenState.value = ScreenState.data()

                val message = errorInteractor.processAndGetMessage(result.error)
                showSnackbarMessage.call(message)
            }
        }
    }

    fun onRecentlyUsedItemSelected(position: Int) {
        val newSelectedFile = recentlyUsedFiles?.get(position) ?: return

        selectedRecentlyUsedFile = newSelectedFile
        selectedPosition = position
    }

    fun onFabActionClicked(position: Int) {
        when (position) {
            0 -> showNewDatabaseScreenEvent.call()
            1 -> showOpenFileScreenEvent.call()
        }
    }

    private fun createDebugPasswordRulesForAutoFill(): List<PasswordRule> {
        val rules = ArrayList<PasswordRule>()

        if (BuildConfig.DEBUG_FILE_NAME_PATTERNS != null && BuildConfig.DEBUG_PASSWORDS != null) {
            for (idx in BuildConfig.DEBUG_FILE_NAME_PATTERNS.indices) {
                val fileNamePattern = BuildConfig.DEBUG_FILE_NAME_PATTERNS[idx]

                val password = BuildConfig.DEBUG_PASSWORDS[idx]
                val pattern = Pattern.compile(fileNamePattern)

                rules.add(
                    PasswordRule(
                        pattern,
                        password
                    )
                )
            }
        }

        return rules
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        val FACTORY = object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return UnlockViewModel(
                    GlobalInjector.get(),
                    GlobalInjector.get(),
                    GlobalInjector.get(),
                    GlobalInjector.get(),
                    GlobalInjector.get()
                ) as T
            }
        }
    }
}
