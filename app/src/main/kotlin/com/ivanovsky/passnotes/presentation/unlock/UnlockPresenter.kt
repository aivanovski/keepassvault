package com.ivanovsky.passnotes.presentation.unlock

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveEvent
import kotlinx.coroutines.*
import javax.inject.Inject

class UnlockPresenter(private val view: UnlockContract.View) :
    UnlockContract.Presenter,
    ObserverBus.UsedFileDataSetObserver {

    @Inject
    lateinit var interactor: UnlockInteractor

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var observerBus: ObserverBus

    @Inject
    lateinit var resourceHelper: ResourceHelper

    override val recentlyUsedFiles = MutableLiveData<List<FileDescriptor>>()
    override val selectedRecentlyUsedFile = MutableLiveData<FileDescriptor>()
    override val showGroupsScreenEvent = SingleLiveEvent<Void>()
    override val showNewDatabaseScreenEvent = SingleLiveEvent<Void>()
    override val showOpenFileScreenEvent = SingleLiveEvent<Void>()
    override val showSettingsScreenEvent = SingleLiveEvent<Void>()
    override val showAboutScreenEvent = SingleLiveEvent<Void>()
    override val showDebugMenuScreenEvent = SingleLiveEvent<Void>()
    private var selectedFile: FileDescriptor? = null
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    init {
        Injector.getInstance().appComponent.inject(this)
    }

    override fun start() {
        if (view.screenState.isNotInitialized) {
            view.screenState = ScreenState.loading()
            observerBus.register(this)
            loadData(null)
        }
        closeActiveDatabaseIfNeed()
    }

    override fun destroy() {
        observerBus.unregister(this)
        job.cancel()
    }

    override fun closeActiveDatabaseIfNeed() {
        if (interactor.hasActiveDatabase()) {
            scope.launch {
                val closeResult = withContext(Dispatchers.Default) {
                    interactor.closeActiveDatabase()
                }

                if (closeResult.isFailed) {
                    val message = errorInteractor.processAndGetMessage(closeResult.error)
                    view.screenState = ScreenState.error(message)
                }
            }
        }
    }

    override fun loadData(selectedFile: FileDescriptor?) {
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.getRecentlyOpenedFiles()
            }

            if (result.isSucceededOrDeferred) {
                val files = result.obj
                if (files.isNotEmpty()) {
                    recentlyUsedFiles.value = files

                    if (selectedFile != null) {
                        val selectedPosition =
                            files.indexOfFirst { f -> isFileEqualsByUidAndFsType(f, selectedFile) }
                        if (selectedPosition != -1) {
                            selectedRecentlyUsedFile.value = selectedFile
                        }
                    }

                    view.screenState = ScreenState.data()
                } else {
                    val emptyText = resourceHelper.getString(R.string.no_files_to_open)
                    view.screenState = ScreenState.empty(emptyText)
                }
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                view.screenState = ScreenState.error(message)
            }
        }
    }

    private fun isFileEqualsByUidAndFsType(lhs: FileDescriptor, rhs: FileDescriptor): Boolean {
        return lhs.uid == rhs.uid && lhs.fsType == rhs.fsType
    }

    override fun onUnlockButtonClicked(password: String, file: FileDescriptor) {
        view.hideKeyboard()
        view.screenState = ScreenState.loading()

        val key = KeepassDatabaseKey(password)

        scope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.openDatabase(key, file)
            }

            if (result.isSucceededOrDeferred) {
                showGroupsScreenEvent.call()
                view.screenState = ScreenState.data()
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                view.screenState = ScreenState.error(message)
            }
        }
    }

    override fun onUsedFileDataSetChanged() {
        loadData(null)
    }

    override fun onOpenFileMenuClicked() {
        showOpenFileScreenEvent.call()
    }

    override fun onSettingsMenuClicked() {
        showSettingsScreenEvent.call()
    }

    override fun onAboutMenuClicked() {
        showAboutScreenEvent.call()
    }

    override fun onDebugMenuClicked() {
        showDebugMenuScreenEvent.call()
    }

    override fun onFilePicked(file: FileDescriptor) {
        //called when user select file from built-in file picker
        view.screenState = ScreenState.loading()

        val usedFile = UsedFile()

        usedFile.filePath = file.path
        usedFile.fileUid = file.uid
        usedFile.fsType = file.fsType

        scope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.saveUsedFileWithoutAccessTime(usedFile)
            }

            if (result.isSucceededOrDeferred) {
                loadData(file)

            } else {
                view.screenState = ScreenState.data()

                val message = errorInteractor.processAndGetMessage(result.error)
                view.showSnackbarMessage(message)
            }
        }
    }

    override fun onFileSelectedByUser(file: FileDescriptor) {
        selectedFile = file
        selectedRecentlyUsedFile.value = file
    }
}