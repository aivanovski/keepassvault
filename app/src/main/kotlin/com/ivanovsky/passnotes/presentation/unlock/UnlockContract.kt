package com.ivanovsky.passnotes.presentation.unlock

import androidx.lifecycle.LiveData
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveEvent

class UnlockContract {

    interface View : BaseView<Presenter> {
        fun setRecentlyUsedFiles(files: List<FileDescriptor>)
        fun selectFileInSpinner(file: FileDescriptor)
        fun showGroupsScreen()
        fun showNewDatabaseScreen()
        fun hideKeyboard()
        fun showOpenFileScreen()
        fun showSettingScreen()
        fun showAboutScreen()
        fun showDebugMenuScreen()
    }

    interface Presenter : BasePresenter {
        val recentlyUsedFiles: LiveData<List<FileDescriptor>>
        val selectedRecentlyUsedFile: LiveData<FileDescriptor>
        val showGroupsScreenEvent: SingleLiveEvent<Void>
        val showNewDatabaseScreenEvent: SingleLiveEvent<Void>
        val hideKeyboardEvent: SingleLiveEvent<Void>
        val showOpenFileScreenEvent: SingleLiveEvent<Void>
        val showSettingsScreenEvent: SingleLiveEvent<Void>
        val showAboutScreenEvent: SingleLiveEvent<Void>
        val showDebugMenuScreenEvent: SingleLiveEvent<Void>

        fun loadData(selectedFile: FileDescriptor?)
        fun onFileSelectedByUser(file: FileDescriptor)
        fun onUnlockButtonClicked(password: String, file: FileDescriptor)
        fun onOpenFileMenuClicked()
        fun onSettingsMenuClicked()
        fun onAboutMenuClicked()
        fun onDebugMenuClicked()
        fun onFilePicked(file: FileDescriptor)
    }
}