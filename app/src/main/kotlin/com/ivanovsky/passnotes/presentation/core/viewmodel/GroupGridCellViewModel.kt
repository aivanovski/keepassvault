package com.ivanovsky.passnotes.presentation.core.viewmodel

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.model.GroupCellModel

class GroupGridCellViewModel(
    override val model: GroupCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onClicked() {
        eventProvider.send((CLICK_EVENT to model.id).toEvent())
    }

    fun onLongClicked() {
        eventProvider.send((LONG_CLICK_EVENT to model.id).toEvent())
    }

    companion object {

        val CLICK_EVENT = GroupGridCellViewModel::class.qualifiedName + "_clickEvent"
        val LONG_CLICK_EVENT = GroupGridCellViewModel::class.qualifiedName + "_longClickEvent"
    }
}