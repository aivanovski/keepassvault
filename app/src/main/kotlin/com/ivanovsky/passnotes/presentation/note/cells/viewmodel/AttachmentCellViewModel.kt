package com.ivanovsky.passnotes.presentation.note.cells.viewmodel

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.note.cells.model.AttachmentCellModel

class AttachmentCellViewModel(
    override val model: AttachmentCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onShareButtonClicked() {
        eventProvider.send((SHARE_ICON_CLICK_EVENT to model.id).toEvent())
    }

    fun onClicked() {
        eventProvider.send((CLICK_EVENT to model.id).toEvent())
    }

    fun onLongClicked() {
        eventProvider.send((LONG_CLICK_EVENT to model.id).toEvent())
    }

    companion object {
        val CLICK_EVENT = AttachmentCellViewModel::class.qualifiedName + "_clickEvent"
        val LONG_CLICK_EVENT = AttachmentCellViewModel::class.qualifiedName + "_longClickEvent"
        val SHARE_ICON_CLICK_EVENT =
            AttachmentCellViewModel::class.qualifiedName + "_shareIconClickEvent"
    }
}