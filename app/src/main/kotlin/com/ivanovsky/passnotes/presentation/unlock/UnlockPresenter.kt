package com.ivanovsky.passnotes.presentation.unlock

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.unlock.UnlockFragment.DropDownItem
import com.ivanovsky.passnotes.util.FileUtils
import com.ivanovsky.passnotes.util.Logger
import kotlinx.coroutines.*
import javax.inject.Inject

class UnlockPresenter(private val view: UnlockContract.View) :
    UnlockContract.Presenter,
    ObserverBus.UsedFileDataSetObserver,
    ObserverBus.UsedFileContentObserver {

    @Inject
    lateinit var interactor: UnlockInteractor

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var observerBus: ObserverBus

    @Inject
    lateinit var resourceHelper: ResourceHelper

    private var selectedRecentlyUsedFile: FileDescriptor? = null
    private var selectedPosition: Int? = null
    private var recentlyUsedFiles: List<FileDescriptor>? = null
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    init {
        Injector.getInstance().appComponent.inject(this)
    }

    override fun start() {
        if (view.screenState.isNotInitialized) {
            view.screenState = ScreenState.loading()
            observerBus.register(this)
            loadData(resetSelection = false)
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

    override fun loadData(resetSelection: Boolean) {
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.getRecentlyOpenedFiles()
            }

            if (result.isSucceededOrDeferred) {
                val files = result.obj
                if (files.isNotEmpty()) {
                    recentlyUsedFiles = files

                    // TODO: remove debug code
                    Logger.d(LOG_TAG, "-----")
                    files.forEach { file -> Logger.d(LOG_TAG, "file: " + file.name) }

                    view.setRecentlyUsedItems(createViewItems(files))

                    if (resetSelection) {
                        selectedPosition = null
                        selectedRecentlyUsedFile = null
                    }

                    selectAlreadySelectedOrFirstFile(files)

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

    private fun selectAlreadySelectedOrFirstFile(files: List<FileDescriptor>) {
        val selectedFile = selectedRecentlyUsedFile
        if (selectedFile == null) {
            selectedRecentlyUsedFile = files[0]
            selectedPosition = 0
            view.setSelectedRecentlyUsedItem(0)
        } else {
            val newSelectedPosition = indexOfFile(files, selectedFile)
            if (newSelectedPosition >= 0 && newSelectedPosition != selectedPosition) {
                selectedPosition = newSelectedPosition
                view.setSelectedRecentlyUsedItem(newSelectedPosition)
            } else if (newSelectedPosition == -1) {
                selectedRecentlyUsedFile = files[0]
                selectedPosition = 0
                view.setSelectedRecentlyUsedItem(0)
            }
        }
    }

    private fun createViewItems(files: List<FileDescriptor>): List<DropDownItem> {
        return files.map { file -> DropDownItem(
            FileUtils.getFileNameFromPath(file.path) ?: "*empty*",
            file.path,
            formatFsType(file.fsType)
        )}
    }

    private fun formatFsType(fsType: FSType): String {
        return when (fsType) {
            FSType.DROPBOX -> "Dropbox"
            FSType.REGULAR_FS -> "Device"
        }
    }

    private fun indexOfFile(files: List<FileDescriptor>, fileToFind: FileDescriptor): Int {
        return files.indexOfFirst { file -> isFileEqualsByUidAndFsType(file, fileToFind)}
    }

    private fun isFileEqualsByUidAndFsType(lhs: FileDescriptor, rhs: FileDescriptor): Boolean {
        return lhs.uid == rhs.uid && lhs.fsType == rhs.fsType
    }

    override fun onUnlockButtonClicked(password: String) {
        val selectedFile = selectedRecentlyUsedFile ?: return

        view.hideKeyboard()
        view.screenState = ScreenState.loading()

        val key = KeepassDatabaseKey(password)

        scope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.openDatabase(key, selectedFile)
            }

            if (result.isSucceededOrDeferred) {
                view.showGroupsScreen()
                view.screenState = ScreenState.data()
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                view.screenState = ScreenState.dataWithError(message)
            }
        }
    }

    override fun onUsedFileDataSetChanged() {
        loadData(resetSelection = true)
    }

    override fun onUsedFileContentChanged(usedFileId: Int) {
        loadData(resetSelection = false)
    }

    override fun onOpenFileMenuClicked() {
        view.showOpenFileScreen()
    }

    override fun onSettingsMenuClicked() {
        view.showSettingScreen()
    }

    override fun onAboutMenuClicked() {
        view.showAboutScreen()
    }

    override fun onDebugMenuClicked() {
        view.showDebugMenuScreen()
    }

    override fun onFilePicked(file: FileDescriptor) {
        //called when user select file from built-in file picker
        view.screenState = ScreenState.loading()

        val usedFile = UsedFile()

        usedFile.filePath = file.path
        usedFile.fileUid = file.uid
        usedFile.fsType = file.fsType
        usedFile.addedTime = System.currentTimeMillis()

        scope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.saveUsedFileWithoutAccessTime(usedFile)
            }

            if (result.isSucceededOrDeferred) {
                loadData(resetSelection = false)

            } else {
                view.screenState = ScreenState.data()

                val message = errorInteractor.processAndGetMessage(result.error)
                view.showSnackbarMessage(message)
            }
        }
    }

    override fun onRecentlyUsedItemSelected(position: Int) {
        val newSelectedFile = recentlyUsedFiles?.get(position) ?: return

        selectedRecentlyUsedFile = newSelectedFile
        selectedPosition = position
    }

    companion object {
        val LOG_TAG = UnlockPresenter::class.java.simpleName
    }
}