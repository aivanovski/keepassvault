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
        event.putString(CLICK_EVENT, model.id)
        eventProvider.send(event)
    }

    companion object {

        val CLICK_EVENT = FileCellViewModel::class.jvmName + "_clickEvent"
    }
}