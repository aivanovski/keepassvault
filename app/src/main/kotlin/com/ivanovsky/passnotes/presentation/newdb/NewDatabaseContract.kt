package com.ivanovsky.passnotes.presentation.newdb

import android.arch.lifecycle.LiveData
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction

class NewDatabaseContract {

	interface View : BaseView<Presenter> {
		fun setStorageTypeAndPath(type: String, path: String)
		fun setDoneButtonVisibility(isVisible: Boolean)
		fun showGroupsScreen()
		fun showStorageScreen()
		fun hideKeyboard()
	}

	interface Presenter : BasePresenter {
		val screenState: LiveData<ScreenState>
		val storageTypeAndPath: LiveData<Pair<String, String>>
		val doneButtonVisibility: LiveData<Boolean>
		val showGroupsScreenAction: SingleLiveAction<Void>
		val showStorageScreenAction: SingleLiveAction<Void>
		val hideKeyboardAction: SingleLiveAction<Void>

		fun createNewDatabaseFile(filename: String, password: String)
		fun selectStorage()
		fun onStorageSelected(selectedFile: FileDescriptor)
	}
}