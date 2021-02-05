package com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels

import android.os.Bundle
import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.event.EventProvider
import com.ivanovsky.passnotes.presentation.core_mvvm.model.FileCellModel
import kotlin.reflect.jvm.jvmName

class FileCellViewModel(
    override val model: FileCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    fun onClicked() {
        val event = Bundle()
        event.putString(ITEM_CLICKED_EVENT, model.id)
        eventProvider.send(event)
    }

    companion object {

        val ITEM_CLICKED_EVENT = FileCellViewModel::class.jvmName + "_itemClickedEvent"
    }
}