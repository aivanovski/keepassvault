package com.ivanovsky.passnotes.presentation.newdb

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.FileHelper
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.newdb.NewDatabaseInteractor
import com.ivanovsky.passnotes.injection.DaggerInjector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core_mvvm.event.SingleLiveEvent
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

class NewDatabasePresenter(
    private val view: NewDatabaseContract.View
) : NewDatabaseContract.Presenter {

    @Inject
    lateinit var interactor: NewDatabaseInteractor

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var fileHelper: FileHelper

    @Inject
    lateinit var resourceProvider: ResourceProvider

    override val storageTypeAndPath = MutableLiveData<Pair<String, String>>()
    override val doneButtonVisibility = MutableLiveData<Boolean>()
    override val showGroupsScreenEvent = SingleLiveEvent<Void>()
    override val showStorageScreenEvent = SingleLiveEvent<Void>()
    private var selectedStorageDir: FileDescriptor? = null
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    init {
        DaggerInjector.getInstance().appComponent.inject(this)
    }

    override fun start() {
        if (view.screenState.isNotInitialized) {
            view.screenState = ScreenState.data()
        }
    }

    override fun destroy() {
        job.cancel()
    }

    override fun createNewDatabaseFile(filename: String, password: String) {
        if (selectedStorageDir != null) {
            view.hideKeyboard()
            doneButtonVisibility.value = false
            view.screenState = ScreenState.loading()

            val dbKey = KeepassDatabaseKey(password)
            val dbFile = FileDescriptor.fromParent(selectedStorageDir, "$filename.kdbx")

            scope.launch {
                val result = withContext(Dispatchers.Default) {
                    interactor.createNewDatabaseAndOpen(dbKey, dbFile)
                }

                if (result.isSucceededOrDeferred) {
                    val created = result.obj

                    if (created) {
                        showGroupsScreenEvent.call()
                    } else {
                        val errorText = resourceProvider.getString(R.string.error_was_occurred)
                        view.screenState = ScreenState.dataWithError(errorText)
                        doneButtonVisibility.value = true
                    }
                } else {
                    val message = errorInteractor.processAndGetMessage(result.error)
                    view.screenState = ScreenState.dataWithError(message)
                    doneButtonVisibility.value = true
                }
            }

        } else {
            val errorText = resourceProvider.getString(R.string.storage_is_not_selected)
            view.screenState = ScreenState.dataWithError(errorText)
        }
    }

    override fun selectStorage() {
        showStorageScreenEvent.call()
    }

    override fun onStorageSelected(selectedFile: FileDescriptor) {
        selectedStorageDir = selectedFile

        if (selectedFile.fsType == FSType.REGULAR_FS) {
            val file = File(selectedFile.path)

            if (fileHelper.isLocatedInPrivateStorage(file)) {
                storageTypeAndPath.value =
                    Pair(resourceProvider.getString(R.string.private_storage), selectedFile.path)
            } else {
                storageTypeAndPath.value =
                    Pair(resourceProvider.getString(R.string.public_storage), selectedFile.path)
            }
        } else if (selectedFile.fsType == FSType.DROPBOX) {
            storageTypeAndPath.value =
                Pair(resourceProvider.getString(R.string.dropbox), selectedFile.path)
        }
    }
}