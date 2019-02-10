package com.ivanovsky.passnotes.presentation.group

import androidx.lifecycle.LiveData
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction

class GroupContract {

	interface View: BaseView<Presenter> {
		fun setTitleEditTextError(error: String?)
		fun setDoneButtonVisibility(isVisible: Boolean)
		fun finishScreen()
		fun hideKeyboard()
	}

	interface Presenter: BasePresenter {
		val screenState: LiveData<ScreenState>
		val doneButtonVisibility: LiveData<Boolean>
		val titleEditTextError: LiveData<String?>
		val hideKeyboardAction: SingleLiveAction<Void>
		val finishScreenAction: SingleLiveAction<Void>

		fun createNewGroup(title: String)
	}
}