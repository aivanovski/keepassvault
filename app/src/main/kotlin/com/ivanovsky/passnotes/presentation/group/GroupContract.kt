package com.ivanovsky.passnotes.presentation.group

import androidx.lifecycle.LiveData
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.GlobalSnackbarPresenter
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveEvent

class GroupContract {

	interface View : BaseView<Presenter> {
		fun setTitleEditTextError(error: String?)
		fun setDoneButtonVisibility(isVisible: Boolean)
		fun finishScreen()
		fun hideKeyboard()
	}

	interface Presenter : BasePresenter, GlobalSnackbarPresenter {
		val doneButtonVisibility: LiveData<Boolean>
		val titleEditTextError: LiveData<String?>
		val hideKeyboardEvent: SingleLiveEvent<Void>
		val finishScreenEvent: SingleLiveEvent<Void>

		fun createNewGroup(title: String)
	}
}