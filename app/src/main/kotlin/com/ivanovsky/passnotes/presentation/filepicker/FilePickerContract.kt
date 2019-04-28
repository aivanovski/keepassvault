package com.ivanovsky.passnotes.presentation.filepicker

import androidx.lifecycle.LiveData
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction

class FilePickerContract {

	interface View: BaseView<Presenter> {
		fun setItems(items: List<FilePickerAdapter.Item>)
		fun setDoneButtonVisibility(isVisible: Boolean)
		fun requestPermission(permission: String)
		fun selectFileAndFinish(file: FileDescriptor)
	}

	interface Presenter: BasePresenter {
		val items: LiveData<List<FilePickerAdapter.Item>>
		val screenState: LiveData<ScreenState>
		val doneButtonVisibility: LiveData<Boolean>
		val requestPermissionAction: SingleLiveAction<String>
		val fileSelectedAction: SingleLiveAction<FileDescriptor>
		val snackbarMessageAction: SingleLiveAction<String>

		fun loadData()
		fun onPermissionResult(granted: Boolean)
		fun onItemClicked(position: Int)
		fun onDoneButtonClicked()
	}
}