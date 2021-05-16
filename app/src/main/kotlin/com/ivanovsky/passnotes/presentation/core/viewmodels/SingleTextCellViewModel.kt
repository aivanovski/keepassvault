package com.ivanovsky.passnotes.presentation.core.viewmodels

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.model.SingleTextCellModel
import kotlin.reflect.jvm.jvmName

class SingleTextCellViewModel(
    override val model: SingleTextCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onClicked() {
        eventProvider.send((CLICK_EVENT to model.id).toEvent())
    }

    companion object {

        val CLICK_EVENT = SingleTextCellModel::class.jvmName + "_clickEvent"
    }
}