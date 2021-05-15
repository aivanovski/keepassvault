package com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels

import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core_mvvm.event.EventProvider
import com.ivanovsky.passnotes.presentation.core_mvvm.model.SingleTextCellModel
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