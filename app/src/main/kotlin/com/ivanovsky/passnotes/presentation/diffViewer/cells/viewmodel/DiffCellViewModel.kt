package com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.DiffCellModel

class DiffCellViewModel(
    override val model: DiffCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onClicked() {
        eventProvider.send((CLICK_EVENT to model.eventId).toEvent())
    }

    companion object {
        val CLICK_EVENT = DiffCellViewModel::class.qualifiedName + "_clickEvent"
    }
}