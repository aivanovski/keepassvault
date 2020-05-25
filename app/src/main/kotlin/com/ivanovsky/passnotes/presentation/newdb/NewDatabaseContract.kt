package com.ivanovsky.passnotes.presentation.newdb

import androidx.lifecycle.LiveData
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveEvent

class NewDatabaseContract {

    interface View : BaseView<Presenter> {
        fun setStorageTypeAndPath(type: String, path: String)
        fun setDoneButtonVisibility(isVisible: Boolean)
        fun showGroupsScreen()
        fun showStorageScreen()
    }

    interface Presenter : BasePresenter {
        val storageTypeAndPath: LiveData<Pair<String, String>>
        val doneButtonVisibility: LiveData<Boolean>
        val showGroupsScreenEvent: SingleLiveEvent<Void>
        val showStorageScreenEvent: SingleLiveEvent<Void>

        fun createNewDatabaseFile(filename: String, password: String)
        fun selectStorage()
        fun onStorageSelected(selectedFile: FileDescriptor)
    }
}