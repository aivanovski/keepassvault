package com.ivanovsky.passnotes.presentation.debugmenu

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError.newErrorMessage
import com.ivanovsky.passnotes.data.entity.TestToggles
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.keepass.KeepassImplementation
import com.ivanovsky.passnotes.data.repository.keepass.PasswordKeepassKey
import com.ivanovsky.passnotes.data.repository.settings.OnSettingsChangeListener
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.domain.interactor.debugmenu.DebugMenuInteractor
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.Screens.DiffViewerScreen
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenVisibilityHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.diffViewer.DiffViewerScreenArgs
import com.ivanovsky.passnotes.presentation.diffViewer.model.DiffEntity
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.util.FileUtils
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DebugMenuViewModel(
    private val interactor: DebugMenuInteractor,
    private val resourceProvider: ResourceProvider,
    private val settings: Settings,
    private val router: Router
) : BaseScreenViewModel(
    initialState = ScreenState.data()
),
    OnSettingsChangeListener {

    val screenVisibilityHandler = DefaultScreenVisibilityHandler()
    val filePath = MutableLiveData(EMPTY)
    val password = MutableLiveData(EMPTY)
    val debugServerUrlText = MutableLiveData(EMPTY)
    val debugCredentialsText = MutableLiveData(EMPTY)
    val isSAFButtonsVisible = MutableLiveData(false)
    val isServerUrlVisible = MutableLiveData(false)
    val isCredentialsVisible = MutableLiveData(false)
    val isWriteButtonEnabled = MutableLiveData(false)
    val isOpenDbButtonEnabled = MutableLiveData(false)
    val isEditDbButtonEnabled = MutableLiveData(false)
    val isCloseDbButtonEnabled = MutableLiveData(false)
    val isAddEntryButtonEnabled = MutableLiveData(false)
    val isExternalStorageEnabled = MutableLiveData(settings.isExternalStorageCacheEnabled)
    val isIgnoreSslCertificate = MutableLiveData(!settings.isSslCertificateValidationEnabled)
    val isFakeBiometricEnabled = MutableLiveData(
        settings.testToggles?.isFakeBiometricEnabled ?: false
    )
    val isFakeFileSystemEnabled = MutableLiveData(
        settings.testToggles?.isFakeFileSystemEnabled ?: false
    )
    val showSnackbarEvent = SingleLiveEvent<String>()
    val showSystemFilePickerEvent = SingleLiveEvent<Unit>()
    val showSystemFileCreatorEvent = SingleLiveEvent<Unit>()
    val fileSystemNames = FILE_SYSTEM_TYPES.map { it.getTitle() }
    val keepassImplementationNames = KEEPASS_IMPLEMENTATION_TYPE.map { it.getTitle() }

    private var lastReadDescriptor: FileDescriptor? = null
    private var lastReadFile: File? = null
    private var selectedFsType = FSType.INTERNAL_STORAGE
    private var selectedImplementationType = KeepassImplementation.KOTPASS
    private var uriFileDescriptor: FileDescriptor? = null

    init {
        settings.register(this)
    }

    override fun onCleared() {
        super.onCleared()
        settings.unregister(this)
    }

    override fun onSettingsChanged(pref: SettingsImpl.Pref) {
        if (pref == SettingsImpl.Pref.TEST_TOGGLES) {
            isFakeBiometricEnabled.value = settings.testToggles?.isFakeBiometricEnabled ?: false
            isFakeFileSystemEnabled.value = settings.testToggles?.isFakeFileSystemEnabled ?: false
        }
    }

    fun onScreenStart() {
        loadTestCredentials()
    }

    fun onReadButtonClicked() {
        val inFile = getSelectedFile() ?: return

        setScreenState(ScreenState.data())

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.getFileContent(inFile)
            }

            if (result.isSucceededOrDeferred) {
                onDbFileAvailable(result.obj.first, result.obj.second)

                showSnackbarEvent.call(resourceProvider.getString(R.string.file_read))
            } else {
                setErrorPanelState(result.error)
            }
        }
    }

    private fun onDbFileAvailable(fileDescriptor: FileDescriptor, file: File) {
        lastReadDescriptor = fileDescriptor
        lastReadFile = file

        isWriteButtonEnabled.value = true
        isOpenDbButtonEnabled.value = true
    }

    fun onWriteButtonClicked() {
        val file = lastReadFile ?: return
        val descriptor = lastReadDescriptor ?: return

        setScreenState(ScreenState.data())

        if (isFileSelected()) {
            viewModelScope.launch {
                val newPath = filePath.value ?: ""
                val newDescriptor = descriptor.copy(
                    uid = newPath,
                    path = newPath,
                    modified = file.lastModified()
                )

                val result = withContext(Dispatchers.Default) {
                    interactor.writeDbFile(file, newDescriptor)
                }

                if (result.isSucceededOrDeferred) {
                    onDbFileAvailable(result.obj.first, result.obj.second)

                    showSnackbarEvent.call(resourceProvider.getString(R.string.file_wrote))
                } else {
                    setErrorPanelState(result.error)
                }
            }
        } else {
            setErrorPanelState(
                newErrorMessage(
                    resourceProvider.getString(R.string.file_is_not_loaded),
                    Stacktrace()
                )
            )
        }
    }

    fun onNewButtonClicked() {
        val outFile = getSelectedFile() ?: return
        val password = this.password.value ?: return

        setScreenState(ScreenState.data())

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.newDbFile(
                    selectedImplementationType,
                    defaultPasswordIfEmpty(password),
                    outFile
                )
            }

            if (result.isSucceededOrDeferred) {
                onDbFileAvailable(result.obj.first, result.obj.second)

                showSnackbarEvent.call(resourceProvider.getString(R.string.new_file_created))
            } else {
                setErrorPanelState(result.error)
            }
        }
    }

    private fun defaultPasswordIfEmpty(password: String): String {
        return if (!password.isBlank()) password else DEFAULT_PASSWORD
    }

    fun onCheckExistsButtonClicked() {
        val file = getSelectedFile() ?: return

        setScreenState(ScreenState.data())

        viewModelScope.launch {
            val result = interactor.isFileExists(file)

            if (result.isSucceededOrDeferred) {
                val isFileExist = result.obj

                showSnackbarEvent.call(
                    resourceProvider.getString(R.string.file_exits_with_str, isFileExist.toString())
                )
            } else {
                setErrorPanelState(result.error)
            }
        }
    }

    fun onOpenDbButtonClicked() {
        val password = this.password.value ?: return

        setScreenState(ScreenState.data())

        if (isFileSelected()) {
            viewModelScope.launch {
                val result = withContext(Dispatchers.Default) {
                    interactor.openDbFile(
                        selectedImplementationType,
                        defaultPasswordIfEmpty(password),
                        lastReadFile!!
                    )
                }

                if (result.isSucceededOrDeferred) {
                    onDbOpened()

                    showSnackbarEvent.call(resourceProvider.getString(R.string.db_opened))
                } else {
                    setErrorPanelState(result.error)
                }
            }
        } else {
            setErrorPanelState(
                newErrorMessage(
                    resourceProvider.getString(R.string.file_is_not_loaded),
                    Stacktrace()
                )
            )
        }
    }

    private fun onDbOpened() {
        isOpenDbButtonEnabled.value = false
        isEditDbButtonEnabled.value = true
        isCloseDbButtonEnabled.value = true
        isAddEntryButtonEnabled.value = true
    }

    private fun isFileSelected(): Boolean {
        return lastReadFile != null && lastReadDescriptor != null
    }

    fun onEditDbButtonClicked() {
        setScreenState(ScreenState.data())

        router.navigateTo(
            GroupsScreen(
                GroupsScreenArgs(
                    appMode = ApplicationLaunchMode.NORMAL,
                    groupUid = null,
                    isCloseDatabaseOnExit = false,
                    isSearchModeEnabled = false
                )
            )
        )
    }

    fun onCloseDbButtonClicked() {
        setScreenState(ScreenState.data())

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.closeDbFile(lastReadFile!!)
            }

            if (result.isSucceededOrDeferred) {
                onDbClosed()

                showSnackbarEvent.call(resourceProvider.getString(R.string.db_closed))
            } else {
                setErrorPanelState(result.error)
            }
        }
    }

    private fun onDbClosed() {
        isOpenDbButtonEnabled.value = true
        isEditDbButtonEnabled.value = false
        isCloseDbButtonEnabled.value = false
        isAddEntryButtonEnabled.value = false
    }

    fun onAddEntryButtonClicked() {
        setScreenState(ScreenState.data())

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.addEntryToDb()
            }

            if (result.isSucceededOrDeferred) {
                showSnackbarEvent.call(resourceProvider.getString(R.string.entry_added))
            } else {
                setErrorPanelState(result.error)
            }
        }
    }

    fun onFileSystemSelected(index: Int) {
        val fsType = FILE_SYSTEM_TYPES[index]
        selectedFsType = fsType

        isServerUrlVisible.value = (fsType == FSType.WEBDAV || fsType == FSType.GIT)
        isCredentialsVisible.value = (fsType == FSType.WEBDAV)
        isSAFButtonsVisible.value = (fsType == FSType.SAF)

        loadTestCredentials()
    }

    fun onExternalStorageCheckBoxChanged(isChecked: Boolean) {
        settings.isExternalStorageCacheEnabled = isChecked
    }

    fun onIgnoreSslCertificateCheckBoxChanged(isChecked: Boolean) {
        settings.isSslCertificateValidationEnabled = !isChecked
    }

    fun onFakeBiometricCheckBoxChanged(isChecked: Boolean) {
        val toggles = settings.testToggles ?: TestToggles()

        settings.testToggles = toggles.copy(
            isFakeBiometricEnabled = isChecked
        )
    }

    fun onFakeFileSystemCheckBoxChanged(isChecked: Boolean) {
        val toggles = settings.testToggles ?: TestToggles()

        settings.testToggles = toggles.copy(
            isFakeFileSystemEnabled = isChecked
        )
    }

    fun onPickFileButtonClicked() {
        showSystemFilePickerEvent.call(Unit)
    }

    fun onCreateFileButtonClicked() {
        showSystemFileCreatorEvent.call(Unit)
    }

    fun onFilePicked(uri: Uri) {
        val fsAuthority = getSelectedFsAuthority()
        val path = uri.toString()

        viewModelScope.launch {
            val getFileResult = interactor.getFileByPath(path, fsAuthority)

            if (getFileResult.isSucceededOrDeferred) {
                val file = getFileResult.obj
                uriFileDescriptor = file

                filePath.value = file.path
                showSnackbarEvent.call(
                    resourceProvider.getString(R.string.successfully)
                )
            } else {
                setErrorPanelState(getFileResult.error)
            }
        }
    }

    fun onGetRootButtonClicked() {
        viewModelScope.launch {
            val getRootResult = interactor.getRootFile(getSelectedFsAuthority())

            if (getRootResult.isSucceededOrDeferred) {
                showSnackbarEvent.call(
                    resourceProvider.getString(R.string.successfully)
                )
            } else {
                setErrorPanelState(getRootResult.error)
            }
        }
    }

    fun onKeepassImplementationSelected(index: Int) {
        selectedImplementationType = KEEPASS_IMPLEMENTATION_TYPE[index]
    }

    fun onResetTestDataButtonClicked() {
        settings.testAutofillData = null
        settings.testToggles = null
        showSnackbarEvent.call(resourceProvider.getString(R.string.test_data_removed))
    }

    fun onViewSimpleDiffButtonClicked() {
        showExampleDiff(
            paths = listOf(
                "/demo/passwords.kdbx",
                "/demo/passwords-modified.kdbx"
            ),
            key = PasswordKeepassKey("abc123")
        )
    }

    fun onViewDetailedDiffButtonClicked() {
        showExampleDiff(
            paths = listOf(
                "/examples/detailed-diff.kdbx",
                "/examples/detailed-diff-modified.kdbx"
            ),
            key = PasswordKeepassKey("abc123")
        )
    }

    private fun showExampleDiff(
        paths: List<String>,
        key: EncryptedDatabaseKey
    ) {
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val getFileResult = interactor.getFakeFileSystemFiles(paths)
            if (getFileResult.isSucceededOrDeferred) {
                val files = getFileResult.obj

                val leftFile = files[0]
                val rightFile = files[1]

                router.navigateTo(
                    DiffViewerScreen(
                        DiffViewerScreenArgs(
                            left = DiffEntity.File(key, leftFile),
                            right = DiffEntity.File(key, rightFile),
                            isHoldDatabaseInteraction = false
                        )
                    )
                )
                setScreenState(ScreenState.data())
            } else {
                setErrorPanelState(getFileResult.error)
            }
        }
    }

    private fun getSelectedFile(): FileDescriptor? {
        val filePath = this.filePath.value ?: return null
        if (filePath.isBlank()) {
            return null
        }

        val fsAuthority = getSelectedFsAuthority()
        return if (fsAuthority.type == FSType.SAF) {
            uriFileDescriptor
        } else {
            FileDescriptor(
                fsAuthority = fsAuthority,
                path = filePath,
                uid = filePath,
                name = FileUtils.getFileNameFromPath(filePath),
                isDirectory = false,
                isRoot = false
            )
        }
    }

    private fun getSelectedFsAuthority(): FSAuthority {
        return when (selectedFsType) {
            FSType.INTERNAL_STORAGE -> FSAuthority.INTERNAL_FS_AUTHORITY
            FSType.EXTERNAL_STORAGE -> FSAuthority.EXTERNAL_FS_AUTHORITY
            FSType.SAF -> FSAuthority.SAF_FS_AUTHORITY
            FSType.WEBDAV -> {
                FSAuthority(
                    credentials = interactor.getTestWebDavCredentials(),
                    type = selectedFsType,
                    isBrowsable = true
                )
            }

            FSType.GIT -> {
                FSAuthority(
                    credentials = interactor.getTestGitCredentials(),
                    type = selectedFsType,
                    isBrowsable = true
                )
            }

            FSType.FAKE -> {
                FSAuthority(
                    credentials = null,
                    type = selectedFsType,
                    isBrowsable = true
                )
            }

            FSType.UNDEFINED -> throw IllegalArgumentException()
        }
    }

    private fun loadTestCredentials() {
        val creds = when (selectedFsType) {
            FSType.WEBDAV -> interactor.getTestWebDavCredentials()
            FSType.GIT -> interactor.getTestGitCredentials()
            else -> null
        }

        val (urlText, credsText) = when {
            creds != null && creds is FSCredentials.BasicCredentials -> {
                Pair(creds.url, creds.username + " / " + creds.password)
            }

            creds != null && creds is FSCredentials.GitCredentials -> {
                Pair(creds.url, EMPTY)
            }

            else -> Pair(EMPTY, EMPTY)
        }

        debugServerUrlText.value = resourceProvider.getString(
            R.string.server_url_with_str,
            urlText
        )
        debugCredentialsText.value = resourceProvider.getString(
            R.string.credentials_with_str,
            credsText
        )
    }

    fun navigateBack() = router.exit()

    private fun FSType.getTitle(): String {
        return when (this) {
            FSType.INTERNAL_STORAGE -> resourceProvider.getString(R.string.internal_storage)
            FSType.EXTERNAL_STORAGE -> resourceProvider.getString(R.string.external_storage)
            FSType.SAF -> resourceProvider.getString(R.string.storage_access_framework)
            FSType.WEBDAV -> resourceProvider.getString(R.string.webdav)
            FSType.GIT -> resourceProvider.getString(R.string.git)
            FSType.FAKE -> resourceProvider.getString(R.string.fake_file_system)
            FSType.UNDEFINED -> FSType.UNDEFINED.name
        }
    }

    private fun KeepassImplementation.getTitle(): String {
        val id = when (this) {
            KeepassImplementation.KOTPASS -> R.string.kotpass
        }
        return resourceProvider.getString(id)
    }

    companion object {
        const val DEFAULT_PASSWORD = "abc123"

        private val FILE_SYSTEM_TYPES = listOf(
            FSType.INTERNAL_STORAGE,
            FSType.EXTERNAL_STORAGE,
            FSType.SAF,
            FSType.WEBDAV,
            FSType.GIT
        )

        private val KEEPASS_IMPLEMENTATION_TYPE = listOf(
            KeepassImplementation.KOTPASS
        )
    }
}