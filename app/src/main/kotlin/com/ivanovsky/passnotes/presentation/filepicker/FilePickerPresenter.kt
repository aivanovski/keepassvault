package com.ivanovsky.passnotes.presentation.filepicker

import android.Manifest
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.ScreenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class FilePickerPresenter(
    private val view: FilePickerContract.View,
    private val mode: Mode,
    rootFile: FileDescriptor,
    private val isBrowsingEnabled: Boolean
) : FilePickerContract.Presenter {

    private val interactor: FilePickerInteractor by inject()
    private val errorInteractor: ErrorInteractor by inject()
    private val permissionHelper: PermissionHelper by inject()
    private val resources: ResourceProvider by inject()
    private val dateFormatProvider: DateFormatProvider by inject()
    private val dispatchers: DispatcherProvider by inject()
    private val viewItemMapper by lazy { ViewItemMapper(dateFormatProvider.getShortDateFormat()) }

    private var isPermissionRejected = false
    private var currentDir = rootFile
    private var files: List<FileDescriptor>? = null
    private var items: List<FilePickerAdapter.Item>? = null

    private val job = Job()
    private val scope = CoroutineScope(dispatchers.Main + job)

    override fun start() {
        if (!isPermissionRejected) {
            loadData()
        }
    }

    override fun destroy() {
        job.cancel()
    }

    override fun loadData() {
        view.screenState = ScreenState.loading()
        view.setDoneButtonVisibility(false)

        //TODO: app doesnt need permission for private storage and network storage
        if (permissionHelper.isPermissionGranted(SDCARD_PERMISSION)) {
            scope.launch {
                val files = withContext(dispatchers.Default) {
                    interactor.getFileList(currentDir)
                }

                onFilesLoaded(currentDir, files)
            }
        } else {
            view.requestPermission(SDCARD_PERMISSION)
        }
    }

    private fun onFilesLoaded(dir: FileDescriptor, result: OperationResult<List<FileDescriptor>>) {
        if (result.isSucceededOrDeferred) {
            val unsortedFiles = result.obj

            if (!dir.isRoot && isBrowsingEnabled) {
                scope.launch {
                    val parent = withContext(dispatchers.Default) {
                        interactor.getParent(currentDir)
                    }

                    onParentLoaded(unsortedFiles, parent)
                }

            } else {
                val sortedFiles = sortFiles(unsortedFiles)

                val displayedFiles = if (isBrowsingEnabled) {
                    sortedFiles
                } else {
                    // hide all directories
                    sortedFiles.filter { file -> !file.isDirectory }
                }

                val adapterItems = createAdapterItems(displayedFiles, null)

                items = adapterItems
                files = displayedFiles

                if (adapterItems.isNotEmpty()) {
                    view.setItems(adapterItems)
                    view.screenState = ScreenState.data()
                    view.setDoneButtonVisibility(true)
                } else {
                    view.screenState = ScreenState.empty(resources.getString(R.string.no_items))
                }
            }
        } else {
            val message = errorInteractor.processAndGetMessage(result.error)
            view.screenState = ScreenState.error(message)
        }
    }

    private fun onParentLoaded(
        unsortedFiles: List<FileDescriptor>,
        result: OperationResult<FileDescriptor>
    ) {
        if (result.isSucceededOrDeferred) {
            val parent = result.obj

            val sortedFiles = sortFiles(unsortedFiles).toMutableList()
            sortedFiles.add(0, parent)

            val adapterItems = createAdapterItems(sortedFiles, parent)

            items = adapterItems
            files = sortedFiles

            view.setItems(createAdapterItems(sortedFiles, parent))
            view.screenState = ScreenState.data()
            view.setDoneButtonVisibility(true)
        } else {
            val message = errorInteractor.processAndGetMessage(result.error)
            view.screenState = ScreenState.error(message)
            view.setDoneButtonVisibility(false)
        }
    }

    private fun sortFiles(files: List<FileDescriptor>): List<FileDescriptor> {
        return files.sortedWith(Comparator { lhs, rhs ->
            val result: Int

            if ((lhs.isDirectory && !rhs.isDirectory) || (!lhs.isDirectory && rhs.isDirectory)) {
                result = if (lhs.isDirectory) -1 else 1
            } else {//if files have same type
                result = lhs.name.compareTo(rhs.name)
            }

            result
        })
    }

    private fun createAdapterItems(
        files: List<FileDescriptor>,
        parent: FileDescriptor?
    ): List<FilePickerAdapter.Item> {
        val items = mutableListOf<FilePickerAdapter.Item>()

        for (file in files) {
            if (file == parent) {
                val item = viewItemMapper.map(file)
                items.add(item.copy(title = ".."))
            } else {
                items.add(viewItemMapper.map(file))
            }
        }

        return items
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            loadData()
        } else {
            //TODO: somehow user should see retry button
            isPermissionRejected = true
            view.screenState = ScreenState.error(
                resources.getString(R.string.permission_denied_message)
            )
            view.setDoneButtonVisibility(false)
        }
    }

    override fun onItemClicked(position: Int) {
        val files = this.files ?: return

        val selectedFile = files[position]

        if (selectedFile.isDirectory) {
            currentDir = files[position]

            loadData()
        } else if (mode == Mode.PICK_FILE) {
            val items = this.items ?: return

            val newItems = items.toMutableList()

            val selectedItem = items[position]
            if (selectedItem.selected) {
                newItems[position] = selectedItem.copy(selected = false)
            } else {
                newItems.forEach { item -> item.selected = false }
                newItems[position] = selectedItem.copy(selected = true)
            }

            this.items = newItems
            view.setItems(newItems)
        }
    }

    override fun onDoneButtonClicked() {
        if (mode == Mode.PICK_DIRECTORY) {
            view.selectFileAndFinish(currentDir)

        } else if (mode == Mode.PICK_FILE) {
            if (isAnyFileSelected()) {
                view.selectFileAndFinish(getSelectedFile())
            } else {
                view.showSnackbarMessage(resources.getString(R.string.please_select_any_file))
            }
        }
    }

    private fun isAnyFileSelected(): Boolean {
        val items = this.items ?: return false

        return items.any { item -> item.selected }
    }

    private fun getSelectedFile(): FileDescriptor {
        val items = this.items ?: throw IllegalStateException("No items for selecting")
        val files = this.files ?: throw IllegalStateException("No files")

        val position = items.indexOfFirst { item -> item.selected }
        return files[position]
    }

    companion object {
        private const val SDCARD_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
}