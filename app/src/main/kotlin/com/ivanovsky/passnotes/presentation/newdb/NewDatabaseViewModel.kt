package com.ivanovsky.passnotes.presentation.newdb

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.FileHelper
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.newdb.NewDatabaseInteractor
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.Screens.StorageListScreen
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.groups.GroupsArgs
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

class NewDatabaseViewModel(
    private val interactor: NewDatabaseInteractor,
    private val errorInteractor: ErrorInteractor,
    private val fileHelper: FileHelper,
    private val resourceProvider: ResourceProvider,
    private val router: Router
) : ViewModel() {

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.data())

    val filename = MutableLiveData(EMPTY)
    val password = MutableLiveData(EMPTY)
    val confirmation = MutableLiveData(EMPTY)
    val filenameError = MutableLiveData<String?>(null)
    val passwordError = MutableLiveData<String?>(null)
    val confirmationError = MutableLiveData<String?>(null)
    val storageType = MutableLiveData<String>()
    val storagePath = MutableLiveData(resourceProvider.getString(R.string.not_selected))
    val doneButtonVisibility = MutableLiveData(true)
    val hideKeyboardEvent = SingleLiveEvent<Unit>()

    private var selectedStorageDir: FileDescriptor? = null

    fun createNewDatabaseFile() {
        val filename = this.filename.value ?: return
        val password = this.password.value ?: return
        val confirmation = this.confirmation.value ?: return

        if (!isFieldsValid(filename, password, confirmation)) {
            return
        }

        val storageDir = selectedStorageDir
        if (storageDir == null) {
            val errorText = resourceProvider.getString(R.string.storage_is_not_selected)
            screenState.value = ScreenState.dataWithError(errorText)
            return
        }

        hideKeyboardEvent.call()
        doneButtonVisibility.value = false
        screenState.value = ScreenState.loading()

        val dbKey = KeepassDatabaseKey(password)
        val dbFile = storageDir.copy(
            path = storageDir.path + "/" + "$filename.kdbx"
        )

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.createNewDatabaseAndOpen(dbKey, dbFile)
            }

            if (result.isSucceededOrDeferred) {
                val created = result.obj

                if (created) {
                    router.replaceScreen(GroupsScreen(GroupsArgs(groupUid = null)))
                } else {
                    val errorText = resourceProvider.getString(R.string.error_was_occurred)
                    screenState.value = ScreenState.dataWithError(errorText)
                    doneButtonVisibility.value = true
                }
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                screenState.value = ScreenState.dataWithError(message)
                doneButtonVisibility.value = true
            }
        }
    }

    private fun isFieldsValid(
        filename: String,
        password: String,
        confirmation: String
    ): Boolean {
        if (filename.isBlank() || password.isBlank() || confirmation.isBlank()) {
            filenameError.value = if (filename.isBlank()) {
                resourceProvider.getString(R.string.empty_field)
            } else {
                null
            }

            passwordError.value = if (password.isBlank()) {
                resourceProvider.getString(R.string.empty_field)
            } else {
                null
            }

            confirmationError.value = if (confirmation.isBlank()) {
                resourceProvider.getString(R.string.empty_field)
            } else {
                null
            }

            return false
        }

        if (FILE_NAME_PATTERN.matcher(filename).matches()) {
            filenameError.value = null
        } else {
            filenameError.value =
                resourceProvider.getString(R.string.field_contains_illegal_character)
        }

        if (PASSWORD_PATTERN.matcher(password).matches()) {
            passwordError.value = null
        } else {
            passwordError.value =
                resourceProvider.getString(R.string.field_contains_illegal_character)
        }

        if (password == confirmation) {
            confirmationError.value = null
        } else {
            confirmationError.value =
                resourceProvider.getString(R.string.this_field_should_match_password)
        }

        return filenameError.value == null &&
            passwordError.value == null &&
            confirmationError.value == null
    }

    fun onSelectStorageClicked() {
        router.setResultListener(StorageListScreen.RESULT_KEY) { file ->
            if (file is FileDescriptor) {
                onStorageSelected(file)
            }
        }
        router.navigateTo(StorageListScreen(Action.PICK_STORAGE))
    }

    fun navigateBack() = router.exit()

    private fun onStorageSelected(selectedFile: FileDescriptor) {
        selectedStorageDir = selectedFile

        when (selectedFile.fsAuthority.type) {
            FSType.REGULAR_FS -> {
                val file = File(selectedFile.path)

                if (fileHelper.isLocatedInPrivateStorage(file)) {
                    storageType.value = resourceProvider.getString(R.string.private_storage)
                } else {
                    storageType.value = resourceProvider.getString(R.string.public_storage)
                }
            }
            FSType.DROPBOX -> {
                storageType.value = resourceProvider.getString(R.string.dropbox)
            }
            FSType.WEBDAV -> {
                storageType.value = resourceProvider.getString(R.string.webdav)
            }
        }

        storagePath.value = selectedFile.path
    }

    companion object {

        private val FILE_NAME_PATTERN = Pattern.compile("[\\w-_]{1,50}")
        private val PASSWORD_PATTERN = Pattern.compile("[\\w@#$!%^&+=]{4,20}")
    }
}