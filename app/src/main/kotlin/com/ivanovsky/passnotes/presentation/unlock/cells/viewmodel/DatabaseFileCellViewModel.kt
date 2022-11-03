package com.ivanovsky.passnotes.presentation.unlock.cells.viewmodel

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.unlock.cells.model.DatabaseFileCellModel

class DatabaseFileCellViewModel(
    override val model: DatabaseFileCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onClicked() {
        eventProvider.send((CLICK_EVENT to model.id).toEvent())
    }

    fun onLongClicked() {
        eventProvider.send((LONG_CLICK_EVENT to model.id).toEvent())
    }

    companion object {
        val CLICK_EVENT = DatabaseFileCellViewModel::class.qualifiedName + "_clickEvent"
        val LONG_CLICK_EVENT = DatabaseFileCellViewModel::class.qualifiedName + "_longClickEvent"
    }
}