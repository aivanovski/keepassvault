package com.ivanovsky.passnotes.presentation.history.cells.viewModel

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryDiffCellModel

class HistoryDiffCellViewModel(
    override val model: HistoryDiffCellModel,
    val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onClicked() {
        eventProvider.send((ITEM_CLICK_EVENT to model.eventId).toEvent())
    }

    companion object {
        val ITEM_CLICK_EVENT = HistoryDiffCellViewModel::class.simpleName + "_itemClickEvent"
    }
}