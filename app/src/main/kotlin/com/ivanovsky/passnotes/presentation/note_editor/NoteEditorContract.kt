package com.ivanovsky.passnotes.presentation.note_editor

import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem

object NoteEditorContract {

    enum class LaunchMode {
        NEW,
        EDIT
    }

    interface View : BaseView<Presenter> {
        fun setEditorItems(items: List<BaseDataItem>)
        fun setDoneButtonVisibility(isVisible: Boolean)
    }

    interface Presenter : BasePresenter {
        fun loadData()
        fun onDoneButtonClicked(items: List<BaseDataItem>)
    }
}