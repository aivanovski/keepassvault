package com.ivanovsky.passnotes.presentation.filepicker

import android.arch.lifecycle.LiveData
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.domain.entity.FileListAndParent
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction

class FilePickerContract {

	interface View: BaseView<Presenter> {
	}

	interface Presenter: BasePresenter {
		val items: LiveData<FileListAndParent>
		val screenState: LiveData<ScreenState>
		val doneButtonVisibility: LiveData<Boolean>
		val requestPermissionAction: SingleLiveAction<String>

		fun loadData()
		fun onPermissionResult(granted: Boolean)
		fun onFileSelected(file: FileDescriptor)
	}
}