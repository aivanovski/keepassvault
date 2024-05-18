package com.ivanovsky.passnotes.presentation.core.viewmodel

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.presentation.core.BaseMutableCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.model.NavigationPanelCellModel

class NavigationPanelCellViewModel(
    initModel: NavigationPanelCellModel,
    val eventProvider: EventProvider
) : BaseMutableCellViewModel<NavigationPanelCellModel>(initModel) {

    val items = MutableLiveData(initModel.items)
    val isVisible = MutableLiveData(initModel.isVisible)

    override fun setModel(newModel: NavigationPanelCellModel) {
        super.setModel(newModel)
        items.value = newModel.items
        isVisible.value = newModel.isVisible
    }

    fun onItemClicked(index: Int) {
        eventProvider.send((ITEM_CLICK_EVENT to index).toEvent())
    }

    companion object {
        val ITEM_CLICK_EVENT =
            NavigationPanelCellViewModel::class.simpleName + "_itemClickEvent"
    }
}