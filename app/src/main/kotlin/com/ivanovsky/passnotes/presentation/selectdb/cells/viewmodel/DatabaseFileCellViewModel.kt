package com.ivanovsky.passnotes.presentation.selectdb.cells.viewmodel

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.selectdb.cells.model.DatabaseFileCellModel

class DatabaseFileCellViewModel(
    override val model: DatabaseFileCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onClicked() {
        eventProvider.send((CLICK_EVENT to model.id).toEvent())
    }

    fun onRemoveButtonClicked() {
        eventProvider.send((REMOVE_CLICK_EVENT to model.id).toEvent())
    }

    fun onResolveButtonClicked() {
        eventProvider.send((RESOLVE_CLICK_EVENT to model.id).toEvent())
    }

    companion object {
        val CLICK_EVENT =
            DatabaseFileCellViewModel::class.qualifiedName + "_clickEvent"

        val REMOVE_CLICK_EVENT =
            DatabaseFileCellViewModel::class.qualifiedName + "_removeClickEvent"

        val RESOLVE_CLICK_EVENT =
            DatabaseFileCellViewModel::class.qualifiedName + "_resolveClickEvent"
    }
}