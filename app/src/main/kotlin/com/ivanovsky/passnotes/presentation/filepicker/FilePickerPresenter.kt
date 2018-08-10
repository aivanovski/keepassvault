package com.ivanovsky.passnotes.presentation.filepicker

import android.Manifest
import android.arch.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.entity.FileListAndParent
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class FilePickerPresenter(private val mode: Mode,
                          private val rootFile: FileDescriptor,
                          private val view: FilePickerContract.View) : FilePickerContract.Presenter {

	@Inject
	lateinit var interactor: FilePickerInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	@Inject
	lateinit var permissionHelper: PermissionHelper

	@Inject
	lateinit var resourceHelper: ResourceHelper

	override val items = MutableLiveData<FileListAndParent>()
	override val screenState = MutableLiveData<ScreenState>()
	override val requestPermissionAction = SingleLiveAction<String>()
	override val doneButtonVisibility = SingleLiveAction<Boolean>()

	private val disposables = CompositeDisposable()
	private var currentDir = rootFile
	private var isPermissionRejected  = false

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

	override fun stop() {
		disposables.clear()
	}

	override fun loadData() {
		screenState.value = ScreenState.loading()
		doneButtonVisibility.value = false

		if (permissionHelper.isPermissionGranted(SDCARD_PERMISSION)) {
			val disposable = interactor.getFileList(currentDir)
					.subscribe({ result -> onFilesLoaded(currentDir, result) })

			disposables.add(disposable)
		} else {
			requestPermissionAction.call(SDCARD_PERMISSION)
		}
	}

	private fun onFilesLoaded(dir: FileDescriptor, result: OperationResult<List<FileDescriptor>>) {
		if (result.result != null) {
			val files = result.result

			if (!dir.isRoot) {
				val disposable = interactor.getParent(currentDir)
						.subscribe({ parentResult -> onParentLoaded(files, parentResult)})

				disposables.add(disposable)
			} else {

				items.value = FileListAndParent(files, null)
				screenState.value = ScreenState.data()
				doneButtonVisibility.value = true
			}
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.value = ScreenState.error(message)
			doneButtonVisibility.value = false
		}
	}

	private fun onParentLoaded(files: List<FileDescriptor>, result: OperationResult<FileDescriptor>) {
		if (result.result != null) {
			val parent = result.result

			items.value = FileListAndParent(files, parent)
			screenState.value = ScreenState.data()
			doneButtonVisibility.value = true
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.value = ScreenState.error(message)
			doneButtonVisibility.value = false
		}
	}

	override fun onPermissionResult(granted: Boolean) {
		if (granted) {
			loadData()
		} else {
			//TODO: somehow user should see retry button
			isPermissionRejected = true
			screenState.value = ScreenState.error(
					resourceHelper.getString(R.string.application_requires_external_storage_permission))
			doneButtonVisibility.value = false
		}
	}

	override fun onFileSelected(file: FileDescriptor) {
		if (file.isDirectory) {
			currentDir = file

			loadData()
		}
	}
}