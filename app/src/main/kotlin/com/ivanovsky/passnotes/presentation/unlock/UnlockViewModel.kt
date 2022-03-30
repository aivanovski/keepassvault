package com.ivanovsky.passnotes.presentation.unlock

import androidx.annotation.IdRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor
import com.ivanovsky.passnotes.extensions.toUsedFile
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode.AUTOFILL_AUTHORIZATION
import com.ivanovsky.passnotes.presentation.Screens
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.Screens.NewDatabaseScreen
import com.ivanovsky.passnotes.presentation.Screens.SelectDatabaseScreen
import com.ivanovsky.passnotes.presentation.Screens.StorageListScreen
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.EventProviderImpl
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.menu.ScreenMenuItem
import com.ivanovsky.passnotes.presentation.core.widget.ExpandableFloatingActionButton.OnItemClickListener
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.presentation.note_editor.view.TextTransformationMethod
import com.ivanovsky.passnotes.presentation.selectdb.SelectDatabaseArgs
import com.ivanovsky.passnotes.presentation.server_login.ServerLoginArgs
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.unlock.cells.factory.UnlockCellModelFactory
import com.ivanovsky.passnotes.presentation.unlock.cells.factory.UnlockCellViewModelFactory
import com.ivanovsky.passnotes.presentation.unlock.cells.model.DatabaseCellModel
import com.ivanovsky.passnotes.presentation.unlock.cells.viewmodel.DatabaseCellViewModel
import com.ivanovsky.passnotes.presentation.unlock.model.PasswordRule
import com.ivanovsky.passnotes.util.FileUtils
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf
import java.util.ArrayList
import java.util.regex.Pattern

class UnlockViewModel(
    private val interactor: UnlockInteractor,
    private val errorInteractor: ErrorInteractor,
    private val observerBus: ObserverBus,
    private val resourceProvider: ResourceProvider,
    private val dispatchers: DispatcherProvider,
    private val modelFactory: UnlockCellModelFactory,
    private val viewModelFactory: UnlockCellViewModelFactory,
    private val router: Router,
    private val args: UnlockScreenArgs
) : ViewModel(),
    ObserverBus.UsedFileDataSetObserver,
    ObserverBus.UsedFileContentObserver,
    ObserverBus.SyncProgressStatusObserver {

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.loading())
    val password = MutableLiveData(EMPTY)
    val passwordTransformationMethod = MutableLiveData(TextTransformationMethod.PASSWORD)
    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val showSnackbarMessage = SingleLiveEvent<String>()
    val sendAutofillResponseEvent = SingleLiveEvent<Pair<Note?, AutofillStructure>>()
    val fileCellViewModels = MutableLiveData<List<BaseCellViewModel>>()
    val isFabButtonVisible = MutableLiveData(false)
    val visibleMenuItems = MutableLiveData<List<UnlockMenuItem>>(emptyList())

    val fileCellTypes = ViewModelTypes()
        .add(DatabaseCellViewModel::class, R.layout.cell_database)

    val showResolveConflictDialog = SingleLiveEvent<SyncConflictInfo>()

    val fabItems = FAB_ITEMS
        .map { (_, resId) -> resourceProvider.getString(resId) }

    val fabClickListener = object : OnItemClickListener {
        override fun onItemClicked(position: Int) {
            onFabItemClicked(position)
        }
    }

    private var selectedFile: FileDescriptor? = null
    private var recentlyUsedFiles: List<FileDescriptor>? = null
    private val debugPasswordRules: List<PasswordRule>
    private var errorPanelButtonAction: ErrorPanelButtonAction? = null

    init {
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
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val result = interactor.getRecentlyOpenedFiles()

            if (result.isSucceededOrDeferred) {
                val files = result.obj
                if (files.isNotEmpty()) {
                    recentlyUsedFiles = files

                    if (resetSelection) {
                        selectedFile = null
                    }

                    val selectedFile = takeAlreadySelectedOrFirst(files)
                    if (selectedFile != null) {
                        setSelectedFile(selectedFile)
                    } else {
                        removeSelectedFileCell()
                    }

                    setScreenState(ScreenState.data())
                } else {
                    val emptyText = resourceProvider.getString(R.string.no_databases)
                    setScreenState(ScreenState.empty(emptyText))
                }
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                setScreenState(ScreenState.error(message))
            }
        }
    }

    fun onErrorPanelButtonClicked() {
        val action = errorPanelButtonAction ?: return

        when (action) {
            ErrorPanelButtonAction.RESOLVE_CONFLICT -> {
                onResolveConflictButtonClicked()
            }
            ErrorPanelButtonAction.REMOVE_FILE -> {
                onRemoveFileButtonClicked()
            }
            ErrorPanelButtonAction.AUTHORISATION -> {
                onLoginButtonClicked()
            }
        }
    }

    fun onResolveConflictConfirmed(resolutionStrategy: ConflictResolutionStrategy) {
        val selectFile = selectedFile ?: return

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val resolvedConflict = interactor.resolveConflict(selectFile, resolutionStrategy)

            if (resolvedConflict.isSucceeded) {
                loadData(resetSelection = false)
            } else {
                setScreenState(
                    ScreenState.dataWithError(
                        errorText = errorInteractor.processAndGetMessage(resolvedConflict.error)
                    )
                )
            }
        }
    }

    private fun indexOfFile(files: List<FileDescriptor>, fileToFind: FileDescriptor): Int {
        return files.indexOfFirst { file -> isFileEqualsByUidAndFsType(file, fileToFind) }
    }

    private fun isFileEqualsByUidAndFsType(lhs: FileDescriptor, rhs: FileDescriptor): Boolean {
        return lhs.uid == rhs.uid && lhs.fsAuthority == rhs.fsAuthority
    }

    fun onUnlockButtonClicked() {
        val password = this.password.value ?: return
        val selectedFile = selectedFile ?: return

        hideKeyboardEvent.call()
        setScreenState(ScreenState.loading())

        val key = KeepassDatabaseKey(password)

        viewModelScope.launch {
            val open = interactor.openDatabase(key, selectedFile)

            if (open.isSucceededOrDeferred) {
                onDatabaseUnlocked()
            } else {
                setScreenState(
                    ScreenState.dataWithError(
                        errorText = errorInteractor.processAndGetMessage(open.error)
                    )
                )
            }
        }
    }

    fun onRefreshButtonClicked() {
        loadData(resetSelection = false)
    }

    private suspend fun onDatabaseUnlocked() {
        when (args.appMode) {
            AUTOFILL_AUTHORIZATION -> {
                val structure = args.autofillStructure ?: return

                val autofillNoteResult = interactor.findNoteForAutofill(structure)
                if (autofillNoteResult.isSucceeded) {
                    val note = autofillNoteResult.obj

                    sendAutofillResponseEvent.call(Pair(note, structure))
                } else {
                    setScreenState(
                        ScreenState.dataWithError(
                            errorText = errorInteractor.processAndGetMessage(autofillNoteResult.error)
                        )
                    )
                }
            }
            else -> {
                clearEnteredPassword()

                router.newChain(
                    GroupsScreen(
                        GroupsScreenArgs(
                            appMode = args.appMode,
                            groupUid = null,
                            isCloseDatabaseOnExit = true,
                            autofillStructure = args.autofillStructure,
                            note = args.note
                        )
                    )
                )
                setScreenState(ScreenState.data())
            }
        }
    }

    private fun navigateToFilePicker() {
        router.setResultListener(StorageListScreen.RESULT_KEY) { file ->
            if (file is FileDescriptor) {
                onFilePicked(file)
            }
        }
        router.navigateTo(StorageListScreen(Action.PICK_FILE))
    }

    fun onPasswordVisibilityButtonClicked() {
        val currentTransformation = passwordTransformationMethod.value ?: return

        passwordTransformationMethod.value = when (currentTransformation) {
            TextTransformationMethod.PASSWORD -> TextTransformationMethod.PLANE_TEXT
            TextTransformationMethod.PLANE_TEXT -> TextTransformationMethod.PASSWORD
        }
    }

    private fun onFilePicked(file: FileDescriptor) {
        //called when user select file from built-in file picker
        setScreenState(ScreenState.loading())

        val usedFile = file.toUsedFile(addedTime = System.currentTimeMillis())

        viewModelScope.launch {
            val result = withContext(dispatchers.Default) {
                interactor.saveUsedFileWithoutAccessTime(usedFile)
            }

            if (result.isSucceededOrDeferred) {
                loadData(resetSelection = false)

            } else {
                setScreenState(ScreenState.data())

                val message = errorInteractor.processAndGetMessage(result.error)
                showSnackbarMessage.call(message)
            }
        }
    }

    private fun navigateToSelectDatabaseScreen() {
        val selectedFile = selectedFile ?: return

        router.setResultListener(SelectDatabaseScreen.RESULT_KEY) { file ->
            if (file is FileDescriptor) {
                setSelectedFile(file)
            }
        }
        router.navigateTo(
            SelectDatabaseScreen(
                SelectDatabaseArgs(
                    selectedFile = selectedFile
                )
            )
        )
    }

    private fun onFabItemClicked(position: Int) {
        when (position) {
            FAB_ITEM_NEW_FILE -> router.navigateTo(NewDatabaseScreen())
            FAB_ITEM_OPEN_FILE -> navigateToFilePicker()
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

    private fun closeActiveDatabaseIfNeed() {
        if (interactor.hasActiveDatabase()) {
            viewModelScope.launch {
                val closeResult = withContext(dispatchers.IO) {
                    interactor.closeActiveDatabase()
                }

                if (closeResult.isFailed) {
                    val message = errorInteractor.processAndGetMessage(closeResult.error)
                    setScreenState(ScreenState.error(message))
                }
            }
        }
    }

    private fun takeAlreadySelectedOrFirst(files: List<FileDescriptor>): FileDescriptor? {
        if (files.isEmpty()) return null

        val selectedFile = this.selectedFile

        return if (selectedFile == null) {
            files[0]
        } else {
            val fileIndex = indexOfFile(files, selectedFile)
            if (fileIndex != -1) {
                selectedFile
            } else {
                files[0]
            }
        }
    }

    private fun setSelectedFile(file: FileDescriptor) {
        this.selectedFile = file

        val files = recentlyUsedFiles ?: return

        fillPasswordIfNeed()

        setSelectedFileCell(
            modelFactory.createFileCellModel(
                file = file,
                syncState = null,
                isNextButtonVisible = files.size > 1,
                onFileClicked = { navigateToSelectDatabaseScreen() }
            )
        )

        viewModelScope.launch {
            val syncState = interactor.getSyncState(file)

            onSyncStateReceived(file, syncState)
        }
    }

    private fun fillPasswordIfNeed() {
        if (!BuildConfig.DEBUG) return

        val file = selectedFile ?: return
        val fileNameWithoutExtension = FileUtils.removeFileExtensionsIfNeed(file.name)

        for (passwordRule in debugPasswordRules) {
            if (passwordRule.pattern.matcher(fileNameWithoutExtension).matches()) {
                password.value = passwordRule.password
            }
        }
    }

    override fun onSyncProgressStatusChanged(
        fsAuthority: FSAuthority,
        uid: String,
        status: SyncProgressStatus
    ) {
        val selectedFile = this.selectedFile ?: return

        if (selectedFile.uid == uid && selectedFile.fsAuthority == fsAuthority) {
            viewModelScope.launch {
                val syncState = interactor.getSyncState(selectedFile)
                onSyncStateReceived(selectedFile, syncState)
            }
        }
    }

    private fun onSyncStateReceived(file: FileDescriptor, syncState: SyncState) {
        if (file != selectedFile) return

        val files = recentlyUsedFiles ?: return

        setSelectedFileCell(
            modelFactory.createFileCellModel(
                file = file,
                syncState = syncState,
                isNextButtonVisible = files.size > 1,
                onFileClicked = { navigateToSelectDatabaseScreen() }
            )
        )

        when (syncState.status) {
            SyncStatus.CONFLICT -> {
                setScreenState(
                    ScreenState.dataWithError(
                        errorText = resourceProvider.getString(R.string.sync_conflict_message),
                        errorButtonText = resourceProvider.getString(R.string.resolve)
                    )
                )
                errorPanelButtonAction = ErrorPanelButtonAction.RESOLVE_CONFLICT
            }
            SyncStatus.ERROR -> {
                setScreenState(
                    ScreenState.dataWithError(
                        errorText = resourceProvider.getString(R.string.sync_error_message),
                        errorButtonText = resourceProvider.getString(R.string.remove)
                    )
                )
                errorPanelButtonAction = ErrorPanelButtonAction.REMOVE_FILE
            }
            SyncStatus.AUTH_ERROR -> {
                setScreenState(
                    ScreenState.dataWithError(
                        errorText = resourceProvider.getString(R.string.sync_auth_error_message),
                        errorButtonText = resourceProvider.getString(R.string.login)
                    )
                )
                errorPanelButtonAction = ErrorPanelButtonAction.AUTHORISATION
            }
            else -> {
                errorPanelButtonAction = null
            }
        }
    }

    private fun onResolveConflictButtonClicked() {
        val selectedFile = selectedFile ?: return
        val lastState = screenState.value ?: return

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val conflict = interactor.getSyncConflictInfo(selectedFile)
            if (conflict.isSucceeded) {
                showResolveConflictDialog.call(conflict.obj)
                setScreenState(lastState)
            } else {
                setScreenState(
                    ScreenState.dataWithError(
                        errorText = errorInteractor.processAndGetMessage(conflict.error)
                    )
                )
            }
        }
    }

    private fun onRemoveFileButtonClicked() {
        val selectedFile = selectedFile ?: return
        val lastState = screenState.value ?: return

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val removeResult = interactor.removeFromUsedFiles(selectedFile)
            if (removeResult.isFailed) {
                setScreenState(lastState)
                return@launch
            }

            loadData(resetSelection = true)
        }
    }

    private fun onLoginButtonClicked() {
        val selectedFile = selectedFile ?: return
        val oldFsAuthority = selectedFile.fsAuthority

        router.setResultListener(Screens.ServerLoginScreen.RESULT_KEY) { newFsAuthority ->
            if (newFsAuthority is FSAuthority) {
                onServerLoginSuccess(
                    fileUid = selectedFile.uid,
                    oldFSAuthority = oldFsAuthority,
                    newFsAuthority = newFsAuthority
                )
            }
        }
        router.navigateTo(
            Screens.ServerLoginScreen(
                ServerLoginArgs(
                    fsAuthority = oldFsAuthority
                )
            )
        )
    }

    private fun onServerLoginSuccess(
        fileUid: String,
        oldFSAuthority: FSAuthority,
        newFsAuthority: FSAuthority
    ) {
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val updateResult = interactor.updateUsedFileFsAuthority(
                fileUid,
                oldFSAuthority,
                newFsAuthority
            )

            if (updateResult.isFailed) {
                val message = errorInteractor.processAndGetMessage(updateResult.error)
                setScreenState(ScreenState.dataWithError(message))
                return@launch
            }

            loadData(resetSelection = false)
        }
    }

    private fun setSelectedFileCell(model: DatabaseCellModel) {
        fileCellViewModels.value = listOf(
            viewModelFactory.createCellViewModel(model, EventProviderImpl())
        )
    }

    private fun removeSelectedFileCell() {
        fileCellViewModels.value = listOf()
    }

    private fun clearEnteredPassword() {
        password.value = EMPTY
    }

    private fun setScreenState(state: ScreenState) {
        screenState.value = state
        isFabButtonVisible.value = getFabButtonVisibility()
        visibleMenuItems.value = getVisibleMenuItems()
    }

    private fun getFabButtonVisibility(): Boolean {
        val screenState = this.screenState.value ?: return false

        return (screenState.isDisplayingData || screenState.isDisplayingEmptyState) &&
            args.appMode == ApplicationLaunchMode.NORMAL
    }

    private fun getVisibleMenuItems(): List<UnlockMenuItem> {
        val screenState = this.screenState.value ?: return emptyList()

        return if (screenState.isDisplayingData) {
            listOf(UnlockMenuItem.REFRESH)
        } else {
            emptyList()
        }
    }

    private enum class ErrorPanelButtonAction {
        RESOLVE_CONFLICT,
        REMOVE_FILE,
        AUTHORISATION
    }

    class Factory(
        private val args: UnlockScreenArgs
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return GlobalInjector.get<UnlockViewModel>(
                parametersOf(args)
            ) as T
        }
    }

    enum class UnlockMenuItem(@IdRes override val menuId: Int) : ScreenMenuItem {
        REFRESH(R.id.menu_refresh)
    }

    companion object {

        private const val FAB_ITEM_NEW_FILE = 0
        private const val FAB_ITEM_OPEN_FILE = 1

        private val FAB_ITEMS = listOf(
            FAB_ITEM_NEW_FILE to R.string.new_file,
            FAB_ITEM_OPEN_FILE to R.string.open_file
        )
    }
}
