package com.ivanovsky.passnotes.presentation.core.viewmodels

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.model.GroupCellModel
import kotlin.reflect.jvm.jvmName

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

        val CLICK_EVENT = GroupGridCellViewModel::class.jvmName + "_clickEvent"
        val LONG_CLICK_EVENT = GroupGridCellViewModel::class.jvmName + "_longClickEvent"
    }
}