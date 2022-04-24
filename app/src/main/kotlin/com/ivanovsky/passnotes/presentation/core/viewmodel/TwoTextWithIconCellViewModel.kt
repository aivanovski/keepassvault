package com.ivanovsky.passnotes.presentation.core.viewmodel

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.model.TwoTextWithIconCellModel

class TwoTextWithIconCellViewModel(
    override val model: TwoTextWithIconCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onClicked() {
        eventProvider.send((CLICK_EVENT to model.id).toEvent())
    }

    companion object {
        val CLICK_EVENT = TwoTextWithIconCellViewModel::class.qualifiedName + "_clickEvent"
    }
}