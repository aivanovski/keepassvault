package com.ivanovsky.passnotes.presentation.history.cells.viewModel

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryHeaderCellModel

class HistoryHeaderCellViewModel(
    override val model: HistoryHeaderCellModel,
    val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onClicked() {
        eventProvider.send((ITEM_CLICK_EVENT to model.itemId).toEvent())
    }

    companion object {
        val ITEM_CLICK_EVENT = HistoryHeaderCellViewModel::class.simpleName + "_itemClickEvent"
    }
}