package com.ivanovsky.passnotes.presentation.unlock

import android.view.inputmethod.EditorInfo
import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.keepass.FileKeepassKey
import com.ivanovsky.passnotes.data.repository.keepass.PasswordKeepassKey
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.biometric.BiometricInteractor
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor
import com.ivanovsky.passnotes.extensions.getKeyFileDescriptor
import com.ivanovsky.passnotes.extensions.getFileDescriptor
import com.ivanovsky.passnotes.extensions.toUsedFile
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode.AUTOFILL_AUTHORIZATION
import com.ivanovsky.passnotes.presentation.Screens
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.Screens.NewDatabaseScreen
import com.ivanovsky.passnotes.presentation.Screens.StorageListScreen
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.EventProviderImpl
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.widget.ExpandableFloatingActionButton.OnItemClickListener
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.presentation.server_login.ServerLoginArgs
import com.ivanovsky.passnotes.presentation.server_login.model.LoginType
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListArgs
import com.ivanovsky.passnotes.presentation.unlock.cells.factory.UnlockCellModelFactory
import com.ivanovsky.passnotes.presentation.unlock.cells.factory.UnlockCellViewModelFactory
import com.ivanovsky.passnotes.presentation.unlock.cells.viewmodel.DatabaseFileCellViewModel
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf

class UnlockViewModel(
    private val interactor: UnlockInteractor,
    private val biometricInteractor: BiometricInteractor,
    private val errorInteractor: ErrorInteractor,
    private val fileSystemResolver: FileSystemResolver,
    private val observerBus: ObserverBus,
    private val resourceProvider: ResourceProvider,
    private val dispatchers: DispatcherProvider,
    private val modelFactory: UnlockCellModelFactory,
    private val viewModelFactory: UnlockCellViewModelFactory,
    private val settings: Settings,
    private val router: Router,
    private val args: UnlockScreenArgs
) : ViewModel(),
    ObserverBus.UsedFileDataSetObserver,
    ObserverBus.UsedFileContentObserver,
    ObserverBus.SyncProgressStatusObserver {

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.loading())
    val filename = MutableLiveData(EMPTY)
    val keyFilename = MutableLiveData(EMPTY)
    val password = MutableLiveData(EMPTY)
    val unlockIconResId = MutableLiveData(getUnlockIconResIdInternal())
    val isKeyboardVisibleEvent = SingleLiveEvent<Boolean>()
    val showSnackbarMessage = SingleLiveEvent<String>()
    val sendAutofillResponseEvent = SingleLiveEvent<Pair<Note?, AutofillStructure>>()
    val fileCellViewModels = MutableLiveData<List<BaseCellViewModel>>()
    val isFabButtonVisible = MutableLiveData(false)
    val showResolveConflictDialog = SingleLiveEvent<SyncConflictInfo>()
    val isAddKeyButtonVisible = MutableLiveData(false)
    val showBiometricUnlockDialog = SingleLiveEvent<BiometricDecoder>()
    val showFileActionsDialog = SingleLiveEvent<UsedFile>()

    val fileCellTypes = ViewModelTypes()
        .add(DatabaseFileCellViewModel::class, R.layout.cell_database_file_2)

    val fabItems = FAB_ITEMS
        .map { (_, resId) -> resourceProvider.getString(resId) }

    val fabClickListener = object : OnItemClickListener {
        override fun onItemClicked(position: Int) {
            onFabItemClicked(position)
        }
    }

    private val eventProvider = EventProviderImpl()
    private var selectedUsedFile: UsedFile? = null
    private var selectedKeyFile: FileDescriptor? = null
    private var recentlyUsedDescriptors: List<FileDescriptor>? = null
    private var recentlyUsedFiles: List<UsedFile>? = null
    private var errorPanelButtonAction: ErrorPanelButtonAction? = null
    private var usedFileIdToSyncStateMap: MutableMap<Int?, SyncState?> = HashMap()

    init {
        observerBus.register(this)
        subscribeToEvents()
    }

    override fun onCleared() {
        super.onCleared()
        observerBus.unregister(this)
        unsubscribeFromEvents()
    }

    override fun onUsedFileDataSetChanged() {
        loadData(
            isResetSelection = false,
            isShowKeyboard = false
        )
    }

    override fun onUsedFileContentChanged(usedFileId: Int) {
        loadData(
            isResetSelection = false,
            isShowKeyboard = false
        )
    }

    fun onScreenStart() {
        closeActiveDatabaseIfNeed()
    }

    fun loadData(
        isResetSelection: Boolean,
        isShowKeyboard: Boolean
    ) {
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val result = interactor.getRecentlyOpenedFiles()

            if (result.isSucceededOrDeferred) {
                val files = result.obj
                if (files.isNotEmpty()) {
                    recentlyUsedDescriptors = files.map { it.getFileDescriptor() }
                    recentlyUsedFiles = files

                    if (isResetSelection) {
                        selectedUsedFile = null
                    }

                    setSelectedFile(takeAlreadySelectedOrFirst(files))
                    setScreenState(ScreenState.data())

                    if (isShowKeyboard) {
                        isKeyboardVisibleEvent.call(true)
                    }
                } else {
                    val emptyText = resourceProvider.getString(R.string.no_databases)
                    setScreenState(ScreenState.empty(emptyText))
                }
            } else {
                setErrorState(result.error)
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
                onRemoveSelectedFileButtonClicked()
            }
            ErrorPanelButtonAction.AUTHORISATION -> {
                onLoginButtonClicked()
            }
        }
    }

    fun onResolveConflictConfirmed(resolutionStrategy: ConflictResolutionStrategy) {
        val selectFile = selectedUsedFile?.getFileDescriptor() ?: return

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val resolvedConflict = interactor.resolveConflict(selectFile, resolutionStrategy)

            if (resolvedConflict.isSucceeded) {
                loadData(
                    isResetSelection = false,
                    isShowKeyboard = false
                )
            } else {
                setErrorPanelState(resolvedConflict.error)
            }
        }
    }

    fun onUnlockButtonClicked() {
        val selectedFile = selectedUsedFile?.getFileDescriptor() ?: return
        val biometricData = selectedUsedFile?.biometricData
        val selectedKeyFile = selectedKeyFile
        val password = password.value ?: EMPTY

        if (isBiometricAuthenticationAvailable() &&
            biometricData != null &&
            selectedKeyFile == null) {
            val decoder = biometricInteractor.getCipherForDecryption(biometricData)
            showBiometricUnlockDialog.call(decoder)
            return
        }

        val key = when {
            selectedKeyFile != null -> {
                FileKeepassKey(
                    file = selectedKeyFile,
                    password = password.ifEmpty { null },
                    fileSystemProvider = fileSystemResolver.resolveProvider(selectedFile.fsAuthority)
                )
            }
            else -> PasswordKeepassKey(password)
        }

        isKeyboardVisibleEvent.call(false)
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val open = interactor.openDatabase(key, selectedFile)

            if (open.isSucceededOrDeferred) {
                onDatabaseUnlocked()
            } else {
                setErrorPanelState(open.error)
            }
        }
    }

    fun onRefreshButtonClicked() {
        loadData(
            isResetSelection = false,
            isShowKeyboard = false
        )
    }

    fun onAddKeyFileButtonClicked() {
        navigateToFilePickerToSelectKey()
    }

    fun onRemoveKeyFileButtonClicked() {
        val selectedFile = selectedUsedFile ?: return
        if (selectedKeyFile == null) {
            return
        }

        viewModelScope.launch {
            val removeKeyResult = interactor.removeKeyFile(selectedFile)
            if (removeKeyResult.isFailed) {
                setErrorPanelState(removeKeyResult.error)
                return@launch
            }

            selectedKeyFile = null

            loadData(
                isResetSelection = false,
                isShowKeyboard = false
            )
        }
    }

    fun onRemoveFileClicked(file: UsedFile) {
        val descriptor = file.getFileDescriptor()

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val removeResult = interactor.removeFromUsedFiles(descriptor)
            if (removeResult.isFailed) {
                setErrorPanelState(removeResult.error)
                return@launch
            }

            loadData(
                isResetSelection = true,
                isShowKeyboard = false
            )
        }
    }

    fun onBiometricUnlockSuccess(decoder: BiometricDecoder) {
        val selectedUsedFile = selectedUsedFile ?: return
        val biometricData = selectedUsedFile.biometricData ?: return
        val selectedFile = selectedUsedFile.getFileDescriptor()

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val passwordResult = interactor.decodePassword(decoder, biometricData)
            if (passwordResult.isFailed) {
                setErrorPanelState(passwordResult.error)
                return@launch
            }

            val key = PasswordKeepassKey(passwordResult.obj)
            val openResult = interactor.openDatabase(key, selectedFile)
            if (openResult.isFailed) {
                setErrorPanelState(openResult.error)
                return@launch
            }

            onDatabaseUnlocked()
        }
    }

    fun handleEditorAction(actionId: Int) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            onUnlockButtonClicked()
        }
    }

    private fun subscribeToEvents() {
        eventProvider.subscribe(this) { event ->
            event.getInt(DatabaseFileCellViewModel.CLICK_EVENT)?.let { id ->
                onDatabaseFileClicked(id)
            }
            event.getInt(DatabaseFileCellViewModel.LONG_CLICK_EVENT)?.let { id ->
                onDatabaseFileLongClicked(id)
            }
        }
    }

    private fun unsubscribeFromEvents() {
        eventProvider.unSubscribe(this)
    }

    private fun onDatabaseFileClicked(usedFileId: Int) {
        val selectedFile = recentlyUsedFiles?.firstOrNull { it.id == usedFileId } ?: return

        setSelectedFile(selectedFile)
    }

    private fun onDatabaseFileLongClicked(usedFileId: Int) {
        val selectedFile = recentlyUsedFiles?.firstOrNull { it.id == usedFileId } ?: return

        showFileActionsDialog.call(selectedFile)
    }

    private fun checkAndSetSelectedKeyFile(keyFile: FileDescriptor) {
        viewModelScope.launch {
            val getFileResult = interactor.getFile(keyFile.path, keyFile.fsAuthority)
            if (getFileResult.isSucceeded) {
                val checkedFile = getFileResult.obj

                selectedKeyFile = checkedFile
                keyFilename.value = checkedFile.name
            } else {
                keyFilename.value = EMPTY
                setErrorPanelState(getFileResult.error)
            }

            isAddKeyButtonVisible.value = isAddKeyFileButtonVisibleInternal()
        }
    }

    private fun clearKeyInputIfNeed() {
        password.value = EMPTY
        selectedKeyFile = null
        isAddKeyButtonVisible.value = true
        keyFilename.value = EMPTY
    }

    private fun fillKeyInputIfNeed() {
        val selectedKeyFile = selectedKeyFile

        when {
            selectedKeyFile == null -> {
                password.value = EMPTY
                viewModelScope.launch {
                    val filename = selectedUsedFile?.getFileDescriptor()?.name ?: return@launch
                    val testPassword = interactor.getTestPasswordForFile(filename) ?: return@launch
                    password.value = testPassword
                }
            }
            else -> {
                checkAndSetSelectedKeyFile(selectedKeyFile)
            }
        }
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
                    setErrorPanelState(autofillNoteResult.error)
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

    private fun navigateToFilePickerToSelectDatabase() {
        router.setResultListener(StorageListScreen.RESULT_KEY) { file ->
            if (file is FileDescriptor) {
                onDatabaseFilePicked(file)
            }
        }
        router.navigateTo(StorageListScreen(StorageListArgs(Action.PICK_FILE)))
    }

    private fun navigateToFilePickerToSelectKey() {
        router.setResultListener(StorageListScreen.RESULT_KEY) { keyFile ->
            if (keyFile is FileDescriptor) {
                checkAndSetSelectedKeyFile(keyFile)
            }
        }
        router.navigateTo(StorageListScreen(StorageListArgs(Action.PICK_FILE)))
    }

    private fun onDatabaseFilePicked(file: FileDescriptor) {
        // userSelectedKeyType = null

        setScreenState(ScreenState.loading())

        val usedFile = file.toUsedFile(
            addedTime = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val saveResult = interactor.saveUsedFileWithoutAccessTime(usedFile)

            if (saveResult.isSucceededOrDeferred) {
                selectedUsedFile = saveResult.obj

                loadData(
                    isResetSelection = false,
                    isShowKeyboard = false
                )
            } else {
                setScreenState(ScreenState.data())

                val message = errorInteractor.processAndGetMessage(saveResult.error)
                showSnackbarMessage.call(message)
            }
        }
    }

    private fun onFabItemClicked(position: Int) {
        when (position) {
            FAB_ITEM_NEW_FILE -> {
                isKeyboardVisibleEvent.call(false)
                router.navigateTo(NewDatabaseScreen())
            }
            FAB_ITEM_OPEN_FILE -> {
                isKeyboardVisibleEvent.call(false)
                navigateToFilePickerToSelectDatabase()
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
                    setErrorPanelState(closeResult.error)
                }
            }
        }
    }

    private fun takeAlreadySelectedOrFirst(files: List<UsedFile>): UsedFile? {
        if (files.isEmpty()) return null

        val selectedFile = this.selectedUsedFile

        return if (selectedFile == null) {
            files[0]
        } else {
            val fileIndex = files.findByUidAndFsType(selectedFile.fileUid, selectedFile.fsAuthority)
            if (fileIndex != -1) {
                files[fileIndex]
            } else {
                files[0]
            }
        }
    }

    private fun List<UsedFile>.findByUidAndFsType(uid: String, fsAuthority: FSAuthority): Int {
        return this.indexOfFirst { usedFile ->
            usedFile.fileUid == uid && usedFile.fsAuthority == fsAuthority
        }
    }

    private fun setSelectedFile(usedFile: UsedFile?) {
        clearKeyInputIfNeed()

        this.selectedUsedFile = usedFile
        this.selectedKeyFile = if (usedFile?.keyType == KeyType.KEY_FILE) {
            usedFile.getKeyFileDescriptor()
        } else {
            null
        }

        val selectedKeyFile = selectedKeyFile
        val files = recentlyUsedFiles ?: return

        fillKeyInputIfNeed()

        if (usedFile != null) {
            usedFileIdToSyncStateMap[usedFile.id] = null
        }

        val models = modelFactory.createFileModels(files, usedFile, usedFileIdToSyncStateMap)
        fileCellViewModels.value = viewModelFactory.createCellViewModels(models, eventProvider)

        unlockIconResId.value = getUnlockIconResIdInternal()
        isAddKeyButtonVisible.value = isAddKeyFileButtonVisibleInternal()
        filename.value = usedFile?.fileName ?: EMPTY
        keyFilename.value = selectedKeyFile?.name ?: EMPTY

        viewModelScope.launch {
            val descriptor = usedFile?.getFileDescriptor() ?: return@launch
            val syncState = interactor.getSyncState(descriptor)

            onSyncStateReceived(usedFile, descriptor, syncState)
        }
    }

    override fun onSyncProgressStatusChanged(
        fsAuthority: FSAuthority,
        uid: String,
        status: SyncProgressStatus
    ) {
        val file = selectedUsedFile ?: return
        val descriptor = file.getFileDescriptor()

        if (descriptor.uid == uid && descriptor.fsAuthority == fsAuthority) {
            viewModelScope.launch {
                val syncState = interactor.getSyncState(descriptor)
                onSyncStateReceived(file, descriptor, syncState)
            }
        }
    }

    private fun onSyncStateReceived(
        file: UsedFile,
        descriptor: FileDescriptor,
        syncState: SyncState
    ) {
        val files = recentlyUsedFiles ?: return
        val selectedFile = selectedUsedFile ?: return

        usedFileIdToSyncStateMap[file.id] = syncState

        if (descriptor.uid != selectedFile.fileUid ||
            descriptor.fsAuthority != selectedFile.fsAuthority) return

        val models = modelFactory.createFileModels(files, selectedFile, usedFileIdToSyncStateMap)
        fileCellViewModels.value = viewModelFactory.createCellViewModels(models, eventProvider)

        when (syncState.status) {
            SyncStatus.FILE_NOT_FOUND -> {
                setScreenState(
                    ScreenState.dataWithError(
                        errorText = resourceProvider.getString(R.string.sync_file_not_found_message),
                        errorButtonText = resourceProvider.getString(R.string.remove)
                    )
                )
                errorPanelButtonAction = ErrorPanelButtonAction.REMOVE_FILE
            }
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
        val selectedFile = selectedUsedFile?.getFileDescriptor() ?: return
        val lastState = screenState.value ?: return

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val conflict = interactor.getSyncConflictInfo(selectedFile)
            if (conflict.isSucceeded) {
                showResolveConflictDialog.call(conflict.obj)
                setScreenState(lastState)
            } else {
                setErrorPanelState(conflict.error)
            }
        }
    }

    private fun onRemoveSelectedFileButtonClicked() {
        val selectedFile = selectedUsedFile?.getFileDescriptor() ?: return
        val lastState = screenState.value ?: return

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val removeResult = interactor.removeFromUsedFiles(selectedFile)
            if (removeResult.isFailed) {
                setScreenState(lastState)
                return@launch
            }

            loadData(
                isResetSelection = true,
                isShowKeyboard = false
            )
        }
    }

    private fun onLoginButtonClicked() {
        val selectedFile = selectedUsedFile?.getFileDescriptor() ?: return
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
                    fsAuthority = oldFsAuthority,
                    loginType = when (selectedFile.fsAuthority.type) {
                        FSType.WEBDAV -> LoginType.USERNAME_PASSWORD
                        FSType.GIT -> LoginType.GIT
                        else -> throw IllegalStateException()
                    }
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
                setErrorState(updateResult.error)
                return@launch
            }

            loadData(
                isResetSelection = false,
                isShowKeyboard = false
            )
        }
    }

    private fun clearEnteredPassword() {
        password.value = EMPTY
    }

    private fun setErrorState(error: OperationError) {
        setScreenState(
            ScreenState.error(
                errorText = errorInteractor.processAndGetMessage(error)
            )
        )
    }

    private fun setErrorPanelState(error: OperationError) {
        setScreenState(
            ScreenState.dataWithError(
                errorText = errorInteractor.processAndGetMessage(error)
            )
        )
    }

    private fun setScreenState(state: ScreenState) {
        screenState.value = state
        isFabButtonVisible.value = getFabButtonVisibility()
    }

    private fun isAddKeyFileButtonVisibleInternal(): Boolean {
        return selectedKeyFile == null
    }

    private fun getFabButtonVisibility(): Boolean {
        val screenState = this.screenState.value ?: return false

        return (screenState.isDisplayingData || screenState.isDisplayingEmptyState) &&
            args.appMode == ApplicationLaunchMode.NORMAL
    }

    @DrawableRes
    private fun getUnlockIconResIdInternal(): Int {
        return if (isBiometricAuthenticationAvailable() &&
            selectedUsedFile?.biometricData != null &&
            selectedKeyFile == null) {
            R.drawable.ic_fingerprint_white_24dp
        } else {
            R.drawable.ic_lock_open_white_24dp
        }
    }

    private fun isBiometricAuthenticationAvailable(): Boolean {
        return biometricInteractor.isBiometricUnlockAvailable() && settings.isBiometricUnlockEnabled
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
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<UnlockViewModel>(
                parametersOf(args)
            ) as T
        }
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
