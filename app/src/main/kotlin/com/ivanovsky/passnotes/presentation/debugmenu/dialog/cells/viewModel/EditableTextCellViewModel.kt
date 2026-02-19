package com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.viewModel

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.presentation.core.BaseMutableCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.model.EditableTextCellModel

class EditableTextCellViewModel(
    initModel: EditableTextCellModel,
    private val eventProvider: EventProvider
) : BaseMutableCellViewModel<EditableTextCellModel>(initModel) {

    val text = MutableLiveData(initModel.text)
    val hint = MutableLiveData(initModel.hint)

    override fun setModel(newModel: EditableTextCellModel) {
        super.setModel(newModel)
        text.value = newModel.text
        hint.value = newModel.hint
    }

    fun onTextChanged(text: String) {
        eventProvider.send((TEXT_CHANGED_EVENT to text).toEvent())
    }

    companion object {
        val TEXT_CHANGED_EVENT =
            EditableTextCellViewModel::class.qualifiedName + "_textChanged"
    }
}