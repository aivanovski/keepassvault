package com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.note.cells.viewmodel.AttachmentCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.model.AttachmentCellModel

class AttachmentCellViewModel(
    override val model: AttachmentCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onRemoveClicked() {
        eventProvider.send((REMOVE_ICON_CLICK_EVENT to model.id).toEvent())
    }

    companion object {
        val REMOVE_ICON_CLICK_EVENT =
            AttachmentCellViewModel::class.qualifiedName + "_removeIconClickEvent"
    }
}