package com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core_mvvm.event.EventProvider
import com.ivanovsky.passnotes.presentation.core_mvvm.model.NotePropertyCellModel
import kotlin.reflect.jvm.jvmName

class NotePropertyCellViewModel(
    override val model: NotePropertyCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    val isValueHidden = MutableLiveData(model.isValueHidden)

    fun onVisibilityButtonClicked() {
        isValueHidden.value = !(isValueHidden.value ?: false)
    }

    fun onCopyButtonClicked() {
        eventProvider.send((COPY_BUTTON_CLICK_EVENT to model.value).toEvent())
    }

    companion object {

        val COPY_BUTTON_CLICK_EVENT = NotePropertyCellViewModel::class.jvmName + "_copyTextClickEvent"
    }
}