package com.ivanovsky.passnotes.presentation.core.viewmodel

import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.model.HeaderCellModel

class HeaderCellViewModel(
    override val model: HeaderCellModel,
    private val eventProvider: EventProvider,
    resourceProvider: ResourceProvider
) : BaseCellViewModel(model) {

    val paddingHorizontal: Int = if (model.paddingHorizontal != null) {
        resourceProvider.getDimension(model.paddingHorizontal)
    } else {
        0
    }

    fun onClicked() {
        eventProvider.send((ITEM_CLICK_EVENT to model.id).toEvent())
    }

    companion object {
        val ITEM_CLICK_EVENT = HeaderCellViewModel::class.simpleName + "_itemClickEvent"
    }
}