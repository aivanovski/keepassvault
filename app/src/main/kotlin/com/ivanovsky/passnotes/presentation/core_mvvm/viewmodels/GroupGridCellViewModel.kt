package com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels

import android.os.Bundle
import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.event.EventProvider
import com.ivanovsky.passnotes.presentation.core_mvvm.model.GroupCellModel
import kotlin.reflect.jvm.jvmName

class GroupGridCellViewModel(
    override val model: GroupCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onClicked() {
        val event = Bundle().apply {
            putString(CLICK_EVENT, model.id)
        }
        eventProvider.send(event)
    }

    fun onLongClicked() {
        val event = Bundle().apply {
            putString(LONG_CLICK_EVENT, model.id)
        }
        eventProvider.send(event)
    }

    companion object {

        val CLICK_EVENT = GroupGridCellViewModel::class.jvmName + "_clickEvent"
        val LONG_CLICK_EVENT = GroupGridCellViewModel::class.jvmName + "_longClickEvent"
    }
}