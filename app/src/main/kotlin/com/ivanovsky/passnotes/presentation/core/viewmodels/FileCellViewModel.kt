package com.ivanovsky.passnotes.presentation.core.viewmodels

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.model.FileCellModel
import kotlin.reflect.jvm.jvmName

class FileCellViewModel(
    override val model: FileCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onClicked() {
        eventProvider.send((CLICK_EVENT to model.id).toEvent())
    }

    companion object {

        val CLICK_EVENT = FileCellViewModel::class.jvmName + "_clickEvent"
    }
}