package com.ivanovsky.passnotes.presentation.filepicker

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView

class FilePickerContract {

    interface View : BaseView<Presenter> {
        fun setItems(items: List<FilePickerAdapter.Item>)
        fun setDoneButtonVisibility(isVisible: Boolean)
        fun requestPermission(permission: String)
        fun selectFileAndFinish(file: FileDescriptor)
    }

    interface Presenter : BasePresenter {
        fun loadData()
        fun onPermissionResult(granted: Boolean)
        fun onItemClicked(position: Int)
        fun onDoneButtonClicked()
    }
}