package com.ivanovsky.passnotes.presentation.storagelist

import android.arch.lifecycle.LiveData
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction

class StorageListContract {

	interface View: BaseView<Presenter> {
	}

	interface Presenter: BasePresenter {
		val storageOptions: LiveData<List<StorageOption>>
		val screenState: LiveData<ScreenState>
		val showFilePickerScreenAction: SingleLiveAction<StorageOption>

		fun selectStorage(option: StorageOption)
	}
}