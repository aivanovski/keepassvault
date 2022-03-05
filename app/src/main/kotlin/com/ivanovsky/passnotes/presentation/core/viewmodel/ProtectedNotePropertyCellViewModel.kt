package com.ivanovsky.passnotes.presentation.core.viewmodel

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.model.ProtectedNotePropertyCellModel

class ProtectedNotePropertyCellViewModel(
    override val model: ProtectedNotePropertyCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    val isValueHidden = MutableLiveData(true)

    fun onVisibilityButtonClicked() {
        isValueHidden.value = !(isValueHidden.value ?: false)
    }

    fun onCopyButtonClicked() {
        eventProvider.send((COPY_BUTTON_CLICK_EVENT to model.value).toEvent())
    }

    companion object {
        val COPY_BUTTON_CLICK_EVENT = ProtectedNotePropertyCellViewModel::class.qualifiedName + "_copyTextClickEvent"
    }
}