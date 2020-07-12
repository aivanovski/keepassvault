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
        fun getEditorItems(): List<BaseDataItem>
        fun setDoneButtonVisibility(isVisible: Boolean)
        fun addEditorItem(item: BaseDataItem)
        fun showDiscardDialog(message: String) // TODO: remove message
    }

    interface Presenter : BasePresenter {
        fun loadData()
        fun onDoneButtonClicked(items: List<BaseDataItem>)
        fun onAddButtonClicked()
        fun onBackPressed()
        fun onDiscardConfirmed()
    }
}