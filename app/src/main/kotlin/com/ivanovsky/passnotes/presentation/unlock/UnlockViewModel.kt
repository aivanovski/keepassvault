package com.ivanovsky.passnotes.presentation.unlock

import android.view.inputmethod.EditorInfo
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.Type.BIOMETRIC_DATA_INVALIDATED_ERROR
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.keepass.FileKeepassKey
import com.ivanovsky.passnotes.data.repository.keepass.PasswordKeepassKey
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.biometric.BiometricResolver
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor
import com.ivanovsky.passnotes.extensions.getFileDescriptor
import com.ivanovsky.passnotes.extensions.getKeyFileDescriptor
import com.ivanovsky.passnotes.extensions.getLoginType
import com.ivanovsky.passnotes.extensions.getOrThrow
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
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.presentation.serverLogin.ServerLoginArgs
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
    private val biometricResolver: BiometricResolver,
    private val errorInteractor: ErrorInteractor,
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
    val unlockButtonColor = MutableLiveData(getUnlockButtonColorInternal())
    val isKeyboardVisibleEvent = SingleLiveEvent<Boolean>()
    val showSnackbarMessage = SingleLiveEvent<String>()
    val showMessageDialog = SingleLiveEvent<String>()
    val sendAutofillResponseEvent = SingleLiveEvent<Pair<Note?, AutofillStructure>>()
    val fileCellViewModels = MutableLiveData<List<BaseCellViewModel>>()
    val isFabButtonVisible = MutableLiveData(false)
    val showResolveConflictDialog = SingleLiveEvent<FileDescriptor>()
    val isAddKeyButtonVisible = MutableLiveData(false)
    val showBiometricUnlockDialog = SingleLiveEvent<BiometricDecoder>()
    val showFileActionsDialog = SingleLiveEvent<UsedFile>()
    val showAddMenuDialog = SingleLiveEvent<Unit>()
    val showUnlockOptionsDialog = SingleLiveEvent<List<UnlockOption>>()

    val fileCellTypes = ViewModelTypes()
        .add(DatabaseFileCellViewModel::class, R.layout.cell_database_file)

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
        unlockButtonColor.value = getUnlockButtonColorInternal()
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

    fun onUnlockButtonClicked() {
        val biometricData = selectedUsedFile?.biometricData
        val selectedKeyFile = selectedKeyFile
        val password = password.value ?: EMPTY

        if (isBiometricAuthenticationAvailable() && biometricData != null) {
            val getDecoderResult = biometricResolver.getInteractor()
                .getCipherForDecryption(biometricData)
            if (getDecoderResult.isSucceeded) {
                showBiometricUnlockDialog.call(getDecoderResult.obj)
            } else {
                removeBiometricData()
            }
            return
        }

        val key = when {
            selectedKeyFile != null -> {
                FileKeepassKey(
                    file = selectedKeyFile,
                    password = password.ifEmpty { null }
                )
            }

            else -> PasswordKeepassKey(password)
        }

        unlock(key)
    }

    fun onUnlockButtonLongClicked() {
        val options = mutableListOf(UnlockOption.PASSWORD)

        if (selectedKeyFile != null) {
            options.add(UnlockOption.KEY_FILE)
            options.add(UnlockOption.PASSWORD_AND_KEY_FILE)
        }

        if (isBiometricAuthenticationAvailable() && selectedUsedFile?.biometricData != null) {
            options.add(UnlockOption.BIOMETRIC)
        }

        showUnlockOptionsDialog.call(options)
    }

    fun onUnlockOptionSelected(option: UnlockOption) {
        when (option) {
            UnlockOption.BIOMETRIC -> {
                onUnlockButtonClicked()
            }

            UnlockOption.PASSWORD -> {
                val key = PasswordKeepassKey(password.value ?: EMPTY)
                unlock(key)
            }

            UnlockOption.KEY_FILE -> {
                val key = selectedKeyFile?.let { keyFile ->
                    FileKeepassKey(
                        file = keyFile
                    )
                } ?: return

                unlock(key)
            }

            UnlockOption.PASSWORD_AND_KEY_FILE -> {
                val key = selectedKeyFile?.let { keyFile ->
                    FileKeepassKey(
                        file = keyFile,
                        password = password.value ?: EMPTY
                    )
                } ?: return

                unlock(key)
            }
        }
    }

    private fun unlock(key: EncryptedDatabaseKey) {
        val selectedFile = selectedUsedFile?.getFileDescriptor() ?: return

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

    private fun removeBiometricData() {
        val biometricData = selectedUsedFile?.biometricData ?: return
        val getDecoderResult = biometricResolver.getInteractor()
            .getCipherForDecryption(biometricData)

        val isBiometricDataInvalidated =
            (getDecoderResult.error.type == BIOMETRIC_DATA_INVALIDATED_ERROR)

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val fileId = selectedUsedFile?.id
            if (fileId == null) {
                loadData(
                    isResetSelection = false,
                    isShowKeyboard = false
                )
                return@launch
            }

            val removeDataResult = interactor.removeBiometricData(fileId)
            if (removeDataResult.isFailed) {
                setErrorState(removeDataResult.error)
                return@launch
            }

            val getFileResult = interactor.getUsedFile(fileId)
            if (getFileResult.isFailed) {
                setErrorState(getFileResult.error)
                return@launch
            }

            selectedUsedFile = getFileResult.obj
            unlockIconResId.value = getUnlockIconResIdInternal()
            setScreenState(ScreenState.data())

            if (isBiometricDataInvalidated) {
                showMessageDialog.call(
                    resourceProvider.getString(
                        R.string.biometric_data_is_invalidated
                    )
                )
            }
        }
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
        val keyFile = selectedKeyFile

        if (selectedUsedFile.keyType == KeyType.KEY_FILE && keyFile == null) return

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val decodePasswordResult = interactor.decodePassword(decoder, biometricData)
            if (decodePasswordResult.isFailed) {
                setErrorPanelState(decodePasswordResult.error)
                return@launch
            }

            val password = decodePasswordResult.getOrThrow()
            val keyType = selectedUsedFile.keyType

            val key = when {
                keyType == KeyType.PASSWORD -> PasswordKeepassKey(password)

                keyType == KeyType.KEY_FILE && keyFile != null -> FileKeepassKey(
                    file = keyFile,
                    password = password
                )

                else -> throw IllegalStateException()
            }

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

    fun onAddButtonClicked() {
        showAddMenuDialog.call(Unit)
    }

    fun onNewFileClicked() {
        isKeyboardVisibleEvent.call(false)
        router.navigateTo(NewDatabaseScreen())
    }

    fun onOpenFileClicked() {
        isKeyboardVisibleEvent.call(false)
        navigateToFilePickerToSelectDatabase()
    }

    fun onEditorAction(actionId: Int) {
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

        selectedKeyFile = null

        setSelectedFile(selectedFile)
        setScreenState(ScreenState.data())
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
        isAddKeyButtonVisible.value = isAddKeyFileButtonVisibleInternal()
        keyFilename.value = selectedKeyFile?.name ?: EMPTY
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
                            isSearchModeEnabled = false,
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
        val resultKey = StorageListScreen.newResultKey()

        router.setResultListener(resultKey) { file ->
            if (file is FileDescriptor) {
                onDatabaseFilePicked(file)
            }
        }

        router.navigateTo(
            StorageListScreen(
                StorageListArgs(
                    action = Action.PICK_FILE,
                    resultKey = resultKey
                )
            )
        )
    }

    private fun navigateToFilePickerToSelectKey() {
        val resultKey = StorageListScreen.newResultKey()

        router.setResultListener(resultKey) { keyFile ->
            if (keyFile is FileDescriptor) {
                checkAndSetSelectedKeyFile(keyFile)
            }
        }

        router.navigateTo(
            StorageListScreen(
                StorageListArgs(
                    action = Action.PICK_FILE,
                    resultKey = resultKey
                )
            )
        )
    }

    private fun onDatabaseFilePicked(file: FileDescriptor) {
        selectedKeyFile = null

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
            selectedKeyFile
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
            descriptor.fsAuthority != selectedFile.fsAuthority
        ) {
            return
        }

        val models = modelFactory.createFileModels(files, selectedFile, usedFileIdToSyncStateMap)
        fileCellViewModels.value = viewModelFactory.createCellViewModels(models, eventProvider)

        val currentScreenState = screenState.value ?: return
        if (currentScreenState.isDisplayingLoading) {
            return
        }

        when (syncState.status) {
            SyncStatus.FILE_NOT_FOUND -> {
                setScreenState(
                    ScreenState.dataWithError(
                        errorText = resourceProvider.getString(
                            R.string.sync_file_not_found_message
                        ),
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
                        errorText = resourceProvider.getString(
                            R.string.sync_auth_error_login_message
                        ),
                        errorButtonText = resourceProvider.getString(R.string.login)
                    )
                )
                errorPanelButtonAction = ErrorPanelButtonAction.AUTHORISATION
            }

            else -> {
                setScreenState(ScreenState.data())
                errorPanelButtonAction = null
            }
        }
    }

    private fun onResolveConflictButtonClicked() {
        val selectedFile = selectedUsedFile?.getFileDescriptor() ?: return

        showResolveConflictDialog.call(selectedFile)
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

        router.setResultListener(Screens.ServerLoginScreen.RESULT_KEY) { file ->
            if (file is FileDescriptor) {
                onServerLoginSuccess(
                    fileUid = selectedFile.uid,
                    oldFSAuthority = oldFsAuthority,
                    newFsAuthority = file.fsAuthority
                )
            }
        }
        router.navigateTo(
            Screens.ServerLoginScreen(
                ServerLoginArgs(
                    fsAuthority = oldFsAuthority,
                    loginType = selectedFile.fsAuthority.type.getLoginType()
                        ?: throw IllegalStateException()
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
            selectedUsedFile?.biometricData != null
        ) {
            R.drawable.ic_fingerprint_24dp
        } else {
            R.drawable.ic_lock_open_24dp
        }
    }

    private fun getUnlockButtonColorInternal(): Int {
        return resourceProvider.getAttributeColor(R.attr.kpPrimaryColor)
    }

    private fun isBiometricAuthenticationAvailable(): Boolean {
        return biometricResolver.getInteractor().isBiometricUnlockAvailable() &&
            settings.isBiometricUnlockEnabled
    }

    private enum class ErrorPanelButtonAction {
        RESOLVE_CONFLICT,
        REMOVE_FILE,
        AUTHORISATION
    }

    enum class UnlockOption(@StringRes val nameResId: Int) {
        PASSWORD(R.string.password),
        KEY_FILE(R.string.key_file),
        PASSWORD_AND_KEY_FILE(R.string.password_and_key_file),
        BIOMETRIC(R.string.biometrics)
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
}