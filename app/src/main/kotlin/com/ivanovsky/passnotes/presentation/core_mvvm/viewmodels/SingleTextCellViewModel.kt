package com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels

import android.os.Bundle
import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.event.EventProvider
import com.ivanovsky.passnotes.presentation.core_mvvm.model.SingleTextCellModel
import kotlin.reflect.jvm.jvmName

class SingleTextCellViewModel(
    val model: SingleTextCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel() {

    fun onClicked() {
        val event = Bundle()
        event.putString(CLICKED_ITEM_ID, model.id)
        eventProvider.send(event)
    }

    companion object {

        val CLICKED_ITEM_ID = SingleTextCellModel::class.jvmName + "_clickedItemId"
    }
}