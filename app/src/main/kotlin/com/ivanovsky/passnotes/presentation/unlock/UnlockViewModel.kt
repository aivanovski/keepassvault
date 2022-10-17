package com.ivanovsky.passnotes.presentation.unlock

import androidx.annotation.IdRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
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
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.unlock.BiometricUnlockInteractor
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
import com.ivanovsky.passnotes.presentation.selectdb.SelectDatabaseArgs
import com.ivanovsky.passnotes.presentation.server_login.ServerLoginArgs
import com.ivanovsky.passnotes.presentation.server_login.model.LoginType
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListArgs
import com.ivanovsky.passnotes.presentation.unlock.cells.factory.UnlockCellModelFactory
import com.ivanovsky.passnotes.presentation.unlock.cells.factory.UnlockCellViewModelFactory
import com.ivanovsky.passnotes.presentation.unlock.cells.model.DatabaseCellModel
import com.ivanovsky.passnotes.presentation.unlock.cells.viewmodel.DatabaseCellViewModel
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf

class UnlockViewModel(
    private val interactor: UnlockInteractor,
    private val biometricInteractor: BiometricUnlockInteractor,
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
    val selectedKeyType = MutableLiveData(KeyType.PASSWORD)
    val selectedKeyTypeTitle = MutableLiveData<String>()
    val password = MutableLiveData(EMPTY)
    val selectedKeyFileTitle = MutableLiveData(resourceProvider.getString(R.string.not_selected))
    val selectedKeyFileTitleColor = MutableLiveData(resourceProvider.getColor(R.color.primary_text))
    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val showSnackbarMessage = SingleLiveEvent<String>()
    val sendAutofillResponseEvent = SingleLiveEvent<Pair<Note?, AutofillStructure>>()
    val fileCellViewModels = MutableLiveData<List<BaseCellViewModel>>()
    val isFabButtonVisible = MutableLiveData(false)
    val visibleMenuItems = MutableLiveData<List<UnlockMenuItem>>(emptyList())
    val allKeyTypes = getKeyTypeNames()
    val showResolveConflictDialog = SingleLiveEvent<SyncConflictInfo>()
    val biometricIconTint = MutableLiveData(getBiometricIconTint())
    val isBiometricAvailable = MutableLiveData(false)
    val showBiometricSetupDialog = SingleLiveEvent<BiometricEncoder>()
    val showBiometricUnlockDialog = SingleLiveEvent<BiometricDecoder>()

    val fileCellTypes = ViewModelTypes()
        .add(DatabaseCellViewModel::class, R.layout.cell_database)

    val fabItems = FAB_ITEMS
        .map { (_, resId) -> resourceProvider.getString(resId) }

    val fabClickListener = object : OnItemClickListener {
        override fun onItemClicked(position: Int) {
            onFabItemClicked(position)
        }
    }

    private var selectedUsedFile: UsedFile? = null
    private var selectedKeyFile: FileDescriptor? = null
    private var userSelectedKeyType: KeyType? = null
    private var recentlyUsedFiles: List<FileDescriptor>? = null
    private var errorPanelButtonAction: ErrorPanelButtonAction? = null

    init {
        observerBus.register(this)
    }

    override fun onCleared() {
        super.onCleared()
        observerBus.unregister(this)
    }

    override fun onUsedFileDataSetChanged() {
        loadData(resetSelection = false)
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
                    val descriptors = files.map { it.getFileDescriptor() }
                    recentlyUsedFiles = descriptors

                    if (resetSelection) {
                        selectedUsedFile = null
                        userSelectedKeyType = null
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
                onRemoveFileButtonClicked()
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
                loadData(resetSelection = false)
            } else {
                setErrorPanelState(resolvedConflict.error)
            }
        }
    }

    fun onUnlockButtonClicked() {
        val selectedKeyType = selectedKeyType.value ?: return
        val selectedFile = selectedUsedFile?.getFileDescriptor() ?: return
        val selectedKeyFile = selectedKeyFile
        val password = password.value ?: EMPTY

        val key = when (selectedKeyType) {
            KeyType.PASSWORD -> PasswordKeepassKey(password)
            KeyType.KEY_FILE -> {
                if (selectedKeyFile == null) {
                    showSnackbarMessage.call(resourceProvider.getString(R.string.key_file_is_not_selected))
                    return
                }

                FileKeepassKey(
                    selectedKeyFile,
                    fileSystemResolver.resolveProvider(selectedFile.fsAuthority)
                )
            }
        }

        hideKeyboardEvent.call()
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
        loadData(resetSelection = false)
    }

    fun onKeyTypeSelected(type: String) {
        val currentKeyType = selectedKeyType.value ?: return
        val newKeyType = getKeyTypeByTitle(type) ?: return

        if (newKeyType == currentKeyType) {
            return
        }

        userSelectedKeyType = newKeyType
        setSelectedKeyType(newKeyType)
        fillKeyInputIfNeed()
    }

    fun onChangeKeyFileButtonClicked() {
        navigateToFilePickerToSelectKey()
    }

    fun onBiometricLoginButtonClicked() {
        val selectedUsedFile = selectedUsedFile ?: return

        if (selectedUsedFile.biometricData != null) {
            showBiometricUnlockDialog.call(
                biometricInteractor.getCipherForDecryption(
                    selectedUsedFile.biometricData
                )
            )
        } else {
            showBiometricSetupDialog.call(biometricInteractor.getCipherForEncryption())
        }
    }

    fun onBiometricSetupSuccess(encoder: BiometricEncoder) {
        val selectedUsedFile = selectedUsedFile ?: return
        val selectedFile = selectedUsedFile.getFileDescriptor()
        val password = password.value ?: EMPTY
        val key = PasswordKeepassKey(password)

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val openResult = interactor.openDatabase(key, selectedFile)
            if (openResult.isFailed) {
                setErrorPanelState(openResult.error)
                return@launch
            }

            val encryptPasswordResult = biometricInteractor.encodeAndStorePasswordForFile(
                encoder,
                password,
                selectedUsedFile
            )
            if (encryptPasswordResult.isFailed) {
                setErrorPanelState(encryptPasswordResult.error)
                return@launch
            }

            onDatabaseUnlocked()
        }
    }

    fun onBiometricUnlockSuccess(decoder: BiometricDecoder) {
        val selectedUsedFile = selectedUsedFile ?: return
        val selectedFile = selectedUsedFile.getFileDescriptor()

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val passwordResult =
                biometricInteractor.decodePassword(decoder, selectedUsedFile)
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

    private fun checkAndSetSelectedKeyFile(file: FileDescriptor) {
        selectedKeyFileTitle.value = resourceProvider.getString(
            R.string.text_with_dots,
            resourceProvider.getString(R.string.checking)
        )
        selectedKeyFileTitleColor.value = resourceProvider.getColor(R.color.secondary_text)

        viewModelScope.launch {
            val getFileResult = interactor.getFile(file.path, file.fsAuthority)
            if (getFileResult.isSucceeded) {
                val checkedFile = getFileResult.obj

                selectedKeyFile = checkedFile
                selectedKeyFileTitle.value = checkedFile.name
                selectedKeyFileTitleColor.value = resourceProvider.getColor(R.color.primary_text)
            } else {
                selectedKeyFileTitle.value = resourceProvider.getString(R.string.not_selected)
                selectedKeyFileTitleColor.value = resourceProvider.getColor(R.color.primary_text)
                setErrorPanelState(getFileResult.error)
            }
        }
    }

    private fun setSelectedKeyType(keyType: KeyType) {
        selectedKeyTypeTitle.value = keyType.getTitle()
        selectedKeyType.value = keyType

        isBiometricAvailable.value = getIsBiometricAvailable()
        biometricIconTint.value = getBiometricIconTint()

        clearKeyInputIfNeed()
    }

    private fun clearKeyInputIfNeed() {
        password.value = EMPTY
        selectedKeyFile = null
        selectedKeyFileTitle.value = EMPTY
    }

    private fun fillKeyInputIfNeed() {
        val selectedKeyType = selectedKeyType.value ?: return
        val selectedKeyFile = selectedKeyFile
        val selectedFile = selectedUsedFile

        when (selectedKeyType) {
            KeyType.PASSWORD -> {
                password.value = EMPTY
                viewModelScope.launch {
                    val filename = selectedUsedFile ?.getFileDescriptor()?.name ?: return@launch
                    val testPassword = interactor.getTestPasswordForFile(filename) ?: return@launch
                    password.value = testPassword
                }
            }
            KeyType.KEY_FILE -> {
                if (selectedKeyFile != null) {
                    checkAndSetSelectedKeyFile(selectedKeyFile)
                } else if (selectedFile != null &&
                    selectedFile.keyType == KeyType.KEY_FILE &&
                    selectedFile.getKeyFileDescriptor() != null
                ) {
                    selectedFile.getKeyFileDescriptor()?.let {
                        checkAndSetSelectedKeyFile(it)
                    }
                } else {
                    selectedKeyFileTitle.value = resourceProvider.getString(R.string.not_selected)
                    selectedKeyFileTitleColor.value =
                        resourceProvider.getColor(R.color.primary_text)
                }
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
        userSelectedKeyType = null

        setScreenState(ScreenState.loading())

        val usedFile = file.toUsedFile(
            addedTime = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val saveResult = interactor.saveUsedFileWithoutAccessTime(usedFile)

            if (saveResult.isSucceededOrDeferred) {
                selectedUsedFile = saveResult.obj

                loadData(resetSelection = false)
            } else {
                setScreenState(ScreenState.data())

                val message = errorInteractor.processAndGetMessage(saveResult.error)
                showSnackbarMessage.call(message)
            }
        }
    }

    private fun navigateToSelectDatabaseScreen() {
        val currentlySelectedFile = selectedUsedFile?.getFileDescriptor() ?: return

        router.setResultListener(SelectDatabaseScreen.RESULT_KEY) { newDbFile ->
            if (newDbFile is FileDescriptor) {
                onDatabaseSelected(newDbFile)
            }
        }
        router.navigateTo(
            SelectDatabaseScreen(
                SelectDatabaseArgs(
                    selectedFile = currentlySelectedFile
                )
            )
        )
    }

    private fun onDatabaseSelected(dbFile: FileDescriptor) {
        userSelectedKeyType = null

        viewModelScope.launch {
            val getUsedFileResult = interactor.getUsedFile(dbFile.uid, dbFile.fsAuthority)
            if (getUsedFileResult.isSucceeded) {
                setSelectedFile(getUsedFileResult.obj)
            } else {
                val message = errorInteractor.processAndGetMessage(getUsedFileResult.error)
                showSnackbarMessage.call(message)
            }
        }
    }

    private fun onFabItemClicked(position: Int) {
        when (position) {
            FAB_ITEM_NEW_FILE -> router.navigateTo(NewDatabaseScreen())
            FAB_ITEM_OPEN_FILE -> navigateToFilePickerToSelectDatabase()
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

    private fun setSelectedFile(usedFile: UsedFile) {
        this.selectedUsedFile = usedFile

        val files = recentlyUsedFiles ?: return

        if (userSelectedKeyType == null) {
            if (usedFile.keyType != selectedKeyType.value) {
                setSelectedKeyType(usedFile.keyType)
            }

            clearKeyInputIfNeed()
            fillKeyInputIfNeed()
        }

        val file = usedFile.getFileDescriptor()

        setSelectedFileCell(
            modelFactory.createFileCellModel(
                file = file,
                syncState = null,
                isNextButtonVisible = files.size > 1,
                onFileClicked = { navigateToSelectDatabaseScreen() }
            )
        )
        isBiometricAvailable.value = getIsBiometricAvailable()
        biometricIconTint.value = getBiometricIconTint()

        viewModelScope.launch {
            val syncState = interactor.getSyncState(file)

            onSyncStateReceived(file, syncState)
        }
    }

    override fun onSyncProgressStatusChanged(
        fsAuthority: FSAuthority,
        uid: String,
        status: SyncProgressStatus
    ) {
        val selectedFile = selectedUsedFile?.getFileDescriptor() ?: return

        if (selectedFile.uid == uid && selectedFile.fsAuthority == fsAuthority) {
            viewModelScope.launch {
                val syncState = interactor.getSyncState(selectedFile)
                onSyncStateReceived(selectedFile, syncState)
            }
        }
    }

    private fun onSyncStateReceived(file: FileDescriptor, syncState: SyncState) {
        if (file.uid != selectedUsedFile?.fileUid) return

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

    private fun onRemoveFileButtonClicked() {
        val selectedFile = selectedUsedFile?.getFileDescriptor() ?: return
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
        visibleMenuItems.value = getVisibleMenuItems()
    }

    private fun getIsBiometricAvailable(): Boolean {
        return selectedKeyType.value == KeyType.PASSWORD &&
            biometricInteractor.isBiometricUnlockAvailable() &&
            settings.isBiometricUnlockEnabled
    }

    private fun getBiometricIconTint(): Int {
        val biometricData = selectedUsedFile?.biometricData
        return if (biometricData != null) {
            resourceProvider.getColor(R.color.icon_gray)
        } else {
            resourceProvider.getColor(R.color.icon_gray_inactive)
        }
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

    private fun getKeyTypeByTitle(title: String): KeyType? {
        return when (title) {
            resourceProvider.getString(R.string.key_file) -> KeyType.KEY_FILE
            resourceProvider.getString(R.string.password) -> KeyType.PASSWORD
            else -> null
        }
    }

    private fun getKeyTypeNames(): List<String> {
        return listOf(KeyType.PASSWORD, KeyType.KEY_FILE)
            .map { it.getTitle() }
    }

    private fun KeyType.getTitle(): String {
        return when (this) {
            KeyType.PASSWORD -> resourceProvider.getString(R.string.password)
            KeyType.KEY_FILE -> resourceProvider.getString(R.string.key_file)
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
