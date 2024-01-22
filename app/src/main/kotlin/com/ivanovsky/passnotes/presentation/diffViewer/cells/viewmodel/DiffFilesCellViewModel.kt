package com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.DiffFilesCellModel

class DiffFilesCellViewModel(
    override val model: DiffFilesCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    val leftTitle = model.leftTitle
    val leftTime = model.leftTime
    val isLeftTimeVisible = model.leftTime.isNotEmpty()

    val rightTitle = model.rightTitle
    val rightTime = model.rightTime
    val isRightTimeVisible = model.rightTime.isNotEmpty()

    fun onChangeButtonClicked() {
        eventProvider.send((CHANGE_BUTTON_CLICK_EVENT to model.id).toEvent())
    }

    companion object {
        val CHANGE_BUTTON_CLICK_EVENT = DiffFilesCellViewModel::class.qualifiedName + "_clickEvent"
    }
}