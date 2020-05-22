package com.ivanovsky.passnotes.presentation.debugmenu

import androidx.lifecycle.LiveData
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveEvent

interface DebugMenuContract {

	interface View : BaseView<Presenter> {
		fun setWriteButtonEnabled(isEnabled: Boolean)
		fun setOpenDbButtonEnabled(isEnabled: Boolean)
		fun setCloseDbButtonEnabled(isEnabled: Boolean)
		fun setAddEntryButtonEnabled(isEnabled: Boolean)
		fun setExternalStorageCheckBoxChecked(isChecked: Boolean)
	}

	interface Presenter : BasePresenter {
		val writeButtonEnabled: LiveData<Boolean>
		val openDbButtonEnabled: LiveData<Boolean>
		val closeDbButtonEnabled: LiveData<Boolean>
		val addEntryButtonEnabled: LiveData<Boolean>
		val externalStorageCheckBoxChecked: LiveData<Boolean>

		fun onReadButtonClicked(inFile: FileDescriptor)
		fun onWriteButtonClicked()
		fun onNewButtonClicked(password: String, outFile: FileDescriptor)
		fun onOpenDbButtonClicked(password: String)
		fun onCloseDbButtonClicked()
		fun onAddEntryButtonClicked()
		fun onExternalStorageCheckedChanged(isChecked: Boolean)
	}
}