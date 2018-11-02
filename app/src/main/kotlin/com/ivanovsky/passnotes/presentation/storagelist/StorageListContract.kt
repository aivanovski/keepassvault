package com.ivanovsky.passnotes.presentation.storagelist

import android.arch.lifecycle.LiveData
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction

class StorageListContract {

	interface View: BaseView<Presenter> {
		fun setStorageOptions(options: List<StorageOption>)
		fun showFilePickerScreen(root: FileDescriptor, action: Action, isBrowsingEnabled: Boolean)
		fun selectFileAndFinish(file: FileDescriptor)
		fun showAuthActivity(fsType: FSType)
	}

	interface Presenter: BasePresenter {
		val storageOptions: LiveData<List<StorageOption>>
		val screenState: LiveData<ScreenState>
		val showFilePickerScreenAction: SingleLiveAction<FilePickerArgs>
		val fileSelectedAction: SingleLiveAction<FileDescriptor>
		val authActivityStartedAction: SingleLiveAction<FSType>

		fun onStorageOptionClicked(option: StorageOption)
		fun onFilePicked(file: FileDescriptor)
	}

	class FilePickerArgs(val root: FileDescriptor, val action: Action, val isBrowsingEnabled: Boolean)
}