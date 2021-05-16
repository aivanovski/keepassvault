package com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels

import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core_mvvm.event.EventProvider
import com.ivanovsky.passnotes.presentation.core_mvvm.model.NoteCellModel
import kotlin.reflect.jvm.jvmName

class NoteGridCellViewModel(
    override val model: NoteCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onClicked() {
        eventProvider.send((CLICK_EVENT to model.id).toEvent())
    }

    fun onLongClicked() {
        eventProvider.send((LONG_CLICK_EVENT to model.id).toEvent())
    }

    companion object {

        val CLICK_EVENT = NoteGridCellViewModel::class.jvmName + "_clickEvent"
        val LONG_CLICK_EVENT = NoteGridCellViewModel::class.jvmName + "_longClickEvent"
    }
}