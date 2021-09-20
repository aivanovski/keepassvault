package com.ivanovsky.passnotes.presentation.debugmenu

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.debugmenu.DebugMenuInteractor
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.groups.GroupsArgs
import com.ivanovsky.passnotes.util.FileUtils
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DebugMenuViewModel(
    private val interactor: DebugMenuInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resourceProvider: ResourceProvider,
    private val settings: Settings,
    private val router: Router
) : ViewModel() {

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.data())

    val filePath = MutableLiveData(EMPTY)
    val password = MutableLiveData(EMPTY)
    val debugServerUrlText = MutableLiveData(EMPTY)
    val debugCredentialsText = MutableLiveData(EMPTY)
    val isPickFileVisible = MutableLiveData(false)
    val isDebugCredentialsVisible = MutableLiveData(false)
    val isWriteButtonEnabled = MutableLiveData(false)
    val isOpenDbButtonEnabled = MutableLiveData(false)
    val isEditDbButtonEnabled = MutableLiveData(false)
    val isCloseDbButtonEnabled = MutableLiveData(false)
    val isAddEntryButtonEnabled = MutableLiveData(false)
    val isExternalStorageEnabled = MutableLiveData(settings.isExternalStorageCacheEnabled)
    val showSnackbarEvent = SingleLiveEvent<String>()
    val showSystemFilePickerEvent = SingleLiveEvent<Unit>()

    private var lastReadDescriptor: FileDescriptor? = null
    private var lastReadFile: File? = null
    private var selectedFsType = FSType.REGULAR_FS
    private var uriFileDescriptor: FileDescriptor? = null

    fun onReadButtonClicked() {
        val inFile = getSelectedFile() ?: return

        screenState.value = ScreenState.data()

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.getFileContent(inFile)
            }

            if (result.isSucceededOrDeferred) {
                onDbFileAvailable(result.obj.first, result.obj.second)

                showSnackbarEvent.call(resourceProvider.getString(R.string.file_read))
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                screenState.value = ScreenState.dataWithError(message)
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

        screenState.value = ScreenState.data()

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
                    val message = errorInteractor.processAndGetMessage(result.error)
                    screenState.value = ScreenState.dataWithError(message)
                }
            }
        } else {
            screenState.value = ScreenState.dataWithError(resourceProvider.getString(R.string.file_is_not_loaded))
        }
    }

    fun onNewButtonClicked() {
        val outFile = getSelectedFile() ?: return
        val password = this.password.value ?: return

        screenState.value = ScreenState.data()

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.newDbFile(defaultPasswordIfEmpty(password), outFile)
            }

            if (result.isSucceededOrDeferred) {
                onDbFileAvailable(result.obj.first, result.obj.second)

                showSnackbarEvent.call(resourceProvider.getString(R.string.new_file_created))
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                screenState.value = ScreenState.dataWithError(message)
            }
        }
    }

    private fun defaultPasswordIfEmpty(password: String): String {
        return if (!password.isBlank()) password else DEFAULT_PASSWORD
    }

    fun onCheckExistsButtonClicked() {
        val file = getSelectedFile() ?: return

        screenState.value = ScreenState.data()

        viewModelScope.launch {
            val result = interactor.isFileExists(file)

            if (result.isSucceededOrDeferred) {
                val isFileExist = result.obj

                showSnackbarEvent.call(
                    resourceProvider.getString(R.string.file_exits_with_str, isFileExist.toString())
                )
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                screenState.value = ScreenState.dataWithError(message)
            }
        }
    }

    fun onOpenDbButtonClicked() {
        val password = this.password.value ?: return

        screenState.value = ScreenState.data()

        if (isFileSelected()) {
            viewModelScope.launch {
                val result = withContext(Dispatchers.Default) {
                    interactor.openDbFile(defaultPasswordIfEmpty(password), lastReadFile!!)
                }

                if (result.isSucceededOrDeferred) {
                    onDbOpened()

                    showSnackbarEvent.call(resourceProvider.getString(R.string.db_opened))
                } else {
                    val message = errorInteractor.processAndGetMessage(result.error)
                    screenState.value = ScreenState.dataWithError(message)
                }
            }
        } else {
            screenState.value = ScreenState.dataWithError(resourceProvider.getString(R.string.file_is_not_loaded))
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
        screenState.value = ScreenState.data()

        router.navigateTo(
            GroupsScreen(
                GroupsArgs(
                    groupUid = null,
                    isCloseDatabaseOnExit = false
                )
            )
        )
    }

    fun onCloseDbButtonClicked() {
        screenState.value = ScreenState.data()

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.closeDbFile(lastReadFile!!)
            }

            if (result.isSucceededOrDeferred) {
                onDbClosed()

                showSnackbarEvent.call(resourceProvider.getString(R.string.db_closed))
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                screenState.value = ScreenState.dataWithError(message)
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
        screenState.value = ScreenState.data()

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.addEntryToDb()
            }

            if (result.isSucceededOrDeferred) {
                showSnackbarEvent.call(resourceProvider.getString(R.string.entry_added))
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                screenState.value = ScreenState.dataWithError(message)
            }
        }
    }

    fun onFileSystemSelected(fsType: FSType) {
        selectedFsType = fsType

        when (fsType) {
            FSType.WEBDAV -> {
                val creds = interactor.getDebugWebDavCredentials()

                val (urlText, credsText) = if (creds != null) {
                    Pair(
                        creds.serverUrl,
                        creds.username + " / " + creds.password
                    )
                } else {
                    Pair(EMPTY, EMPTY)
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
        }

        isDebugCredentialsVisible.value = (fsType == FSType.WEBDAV)
        isPickFileVisible.value = (fsType == FSType.SAF)
    }

    fun onExternalStorageCheckBoxChanged(isChecked: Boolean) {
        settings.isExternalStorageCacheEnabled = isChecked
    }

    fun onPickFileButtonClicked() {
        showSystemFilePickerEvent.call()
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
                val message = errorInteractor.processAndGetMessage(getFileResult.error)
                screenState.value = ScreenState.dataWithError(message)
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
            FSType.REGULAR_FS -> FSAuthority.REGULAR_FS_AUTHORITY
            FSType.SAF -> FSAuthority.SAF_FS_AUTHORITY
            FSType.DROPBOX -> FSAuthority.DROPBOX_FS_AUTHORITY
            FSType.WEBDAV -> {
                val creds = interactor.getDebugWebDavCredentials()
                FSAuthority(creds, selectedFsType)
            }
        }
    }

    fun navigateBack() = router.exit()

    companion object {

        const val DEFAULT_PASSWORD = "abc123"
    }
}