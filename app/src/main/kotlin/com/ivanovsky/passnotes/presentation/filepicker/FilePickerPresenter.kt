package com.ivanovsky.passnotes.presentation.filepicker

import android.Manifest
import androidx.annotation.DrawableRes
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.LocaleProvider
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.util.formatAccordingLocale
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class FilePickerPresenter(
    private val view: FilePickerContract.View,
    private val mode: Mode,
    rootFile: FileDescriptor,
    private val isBrowsingEnabled: Boolean
) : FilePickerContract.Presenter {

    @Inject
    lateinit var interactor: FilePickerInteractor

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var permissionHelper: PermissionHelper

    @Inject
    lateinit var resourceHelper: ResourceHelper

    @Inject
    lateinit var localeProvider: LocaleProvider

    private var isPermissionRejected = false
    private var currentDir = rootFile
    private var files: List<FileDescriptor>? = null
    private var items: List<FilePickerAdapter.Item>? = null

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    companion object {
        private const val SDCARD_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    init {
        Injector.getInstance().appComponent.inject(this)
    }

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
                val files = withContext(Dispatchers.Default) {
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
                    val parent = withContext(Dispatchers.Default) {
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

                view.setItems(adapterItems)
                view.screenState = ScreenState.data()
                view.setDoneButtonVisibility(true)
            }
        } else {
            val message = errorInteractor.processAndGetMessage(result.error)
            view.screenState = ScreenState.error(message)
            view.setDoneButtonVisibility(false)
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
            val iconResId = getIconResId(file.isDirectory)
            val title = if (file == parent) ".." else formatItemTitle(file)
            val description = formatModifiedDate(file.modified)

            items.add(FilePickerAdapter.Item(iconResId, title, description, false))
        }

        return items
    }

    private fun formatModifiedDate(modified: Long?): String {
        return if (modified != null) {
            Date(modified).formatAccordingLocale(localeProvider.getSystemLocale())
        } else {
            ""
        }
    }

    @DrawableRes
    private fun getIconResId(isDirectory: Boolean): Int {
        return if (isDirectory) R.drawable.ic_folder_white_24dp else R.drawable.ic_file_white_24dp
    }

    private fun formatItemTitle(file: FileDescriptor): String {
        return if (file.isDirectory) file.name + "/" else file.name
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            loadData()
        } else {
            //TODO: somehow user should see retry button
            isPermissionRejected = true
            view.screenState = ScreenState.error(
                resourceHelper.getString(R.string.application_requires_external_storage_permission)
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
                view.showSnackbarMessage(resourceHelper.getString(R.string.please_select_any_file))
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
}