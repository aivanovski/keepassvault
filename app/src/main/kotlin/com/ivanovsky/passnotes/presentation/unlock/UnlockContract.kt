package com.ivanovsky.passnotes.presentation.unlock

import android.arch.lifecycle.LiveData
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import java.io.File

class UnlockContract {

	interface View : BaseView<Presenter> {
		fun setRecentlyUsedFiles(files: List<UsedFile>)
		fun showGroupsScreen()
		fun showNewDatabaseScreen()
		fun hideKeyboard()
	}

	interface Presenter : BasePresenter {
		val recentlyUsedFiles: LiveData<List<UsedFile>>
		val screenState: LiveData<ScreenState>
		val showGroupsScreenAction: SingleLiveAction<Void>
		val showNewDatabaseScreenAction: SingleLiveAction<Void>
		val hideKeyboardAction: SingleLiveAction<Void>

		fun loadData()
		fun onUnlockButtonClicked(password: String, dbFile: File)
	}
}