package com.ivanovsky.passnotes.presentation.note.cells.viewmodel

import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.note.cells.model.NotePropertyCellModel
import com.ivanovsky.passnotes.presentation.core.widget.entity.TextTransformationMethod

class NotePropertyCellViewModel(
    initModel: NotePropertyCellModel,
    private val eventProvider: EventProvider
) : BaseCellViewModel(initModel) {

    override var model: NotePropertyCellModel = initModel
        set(newModel) {
            applyModel(newModel)
            field = newModel
        }

    val name = MutableLiveData(initModel.name)
    val value = MutableLiveData(initModel.value)
    val isVisibilityButtonVisible = MutableLiveData(initModel.isVisibilityButtonVisible)
    val valueTransformationMethod = MutableLiveData(
        getTextTransformationMethod(initModel.isValueProtected)
    )
    val visibilityIconResId = MutableLiveData(
        getVisibilityIconResIdInternal(initModel.isValueProtected)
    )

    fun onClicked() {
        eventProvider.send((CLICK_EVENT to model.id).toEvent())
    }

    fun onLongClicked() {
        eventProvider.send((LONG_CLICK_EVENT to model.id).toEvent())
    }

    fun onVisibilityButtonClicked() {
        model = model.copy(
            isValueProtected = !model.isValueProtected
        )
    }

    private fun applyModel(model: NotePropertyCellModel) {
        name.value = model.name
        value.value = model.value
        isVisibilityButtonVisible.value = model.isVisibilityButtonVisible
        valueTransformationMethod.value = getTextTransformationMethod(model.isValueProtected)
        visibilityIconResId.value = getVisibilityIconResIdInternal(model.isValueProtected)
    }

    private fun getTextTransformationMethod(isProtected: Boolean): TextTransformationMethod {
        return if (isProtected) {
            TextTransformationMethod.PASSWORD
        } else {
            TextTransformationMethod.PLANE_TEXT
        }
    }

    @DrawableRes
    private fun getVisibilityIconResIdInternal(isProtected: Boolean): Int {
        return if (isProtected) {
            R.drawable.ic_visibility_on_24dp
        } else {
            R.drawable.ic_visibility_off_24dp
        }
    }

    companion object {
        val CLICK_EVENT = NotePropertyCellViewModel::class.qualifiedName + "_clickEvent"
        val LONG_CLICK_EVENT = NotePropertyCellViewModel::class.qualifiedName + "_longClickEvent"
    }
}