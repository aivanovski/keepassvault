package com.ivanovsky.passnotes.presentation.core.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.ivanovsky.passnotes.presentation.core.BaseMutableCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.model.OptionPanelCellModel

class OptionPanelCellViewModel(
    initModel: OptionPanelCellModel,
    private val eventProvider: EventProvider
) : BaseMutableCellViewModel<OptionPanelCellModel>(initModel) {

    val isVisible = MutableLiveData(initModel.isVisible)
    val positiveText = MutableLiveData(initModel.positiveText)
    val negativeText = MutableLiveData(initModel.negativeText)
    val message = MutableLiveData(initModel.message)
    val isSpaceVisible = Transformations.map(message) { it.isEmpty() }
    val isMessageVisible = Transformations.map(message) { it.isNotEmpty() }

    override fun setModel(newModel: OptionPanelCellModel) {
        super.setModel(newModel)
        isVisible.value = newModel.isVisible
        positiveText.value = newModel.positiveText
        negativeText.value = newModel.negativeText
        message.value = newModel.message
    }

    fun onPositiveButtonClicked() {
        eventProvider.send((POSITIVE_BUTTON_CLICK_EVENT to model.id).toEvent())
    }

    fun onNegativeButtonClicked() {
        eventProvider.send((NEGATIVE_BUTTON_CLICK_EVENT to model.id).toEvent())
    }

    companion object {
        val POSITIVE_BUTTON_CLICK_EVENT =
            OptionPanelCellViewModel::class.simpleName + "_positiveButtonClickEvent"

        val NEGATIVE_BUTTON_CLICK_EVENT =
            OptionPanelCellViewModel::class.simpleName + "_negativeButtonClickEvent"
    }
}