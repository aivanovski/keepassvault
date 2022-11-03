package com.ivanovsky.passnotes.presentation.core.viewmodel

import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.model.ProtectedNotePropertyCellModel

class ProtectedNotePropertyCellViewModel(
    override val model: ProtectedNotePropertyCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(model) {

    val isValueHidden = MutableLiveData(true)
    val visibilityIconResId = MutableLiveData(getVisibilityIconResIdInternal())

    fun onVisibilityButtonClicked() {
        isValueHidden.value = !(isValueHidden.value ?: false)
        visibilityIconResId.value = getVisibilityIconResIdInternal()
    }

    fun onCopyButtonClicked() {
        eventProvider.send((COPY_BUTTON_CLICK_EVENT to model.value).toEvent())
    }

    @DrawableRes
    private fun getVisibilityIconResIdInternal(): Int {
        val isValueVisible = !(isValueHidden.value ?: false)
        return if (isValueVisible) {
            R.drawable.ic_visibility_on_24dp
        } else {
            R.drawable.ic_visibility_off_24dp
        }
    }

    companion object {
        val COPY_BUTTON_CLICK_EVENT = ProtectedNotePropertyCellViewModel::class.qualifiedName + "_copyTextClickEvent"
    }
}