package com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel

import androidx.compose.runtime.Stable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEventProvider
import com.ivanovsky.passnotes.presentation.core.compose.cells.MutableCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.GroupDiffCellEvent
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.GroupDiffCellModel

@Stable
class GroupDiffCellViewModel(
    initialModel: GroupDiffCellModel,
    private val eventProvider: CellEventProvider
) : MutableCellViewModel<GroupDiffCellModel>(initialModel) {

    fun sendEvent(event: GroupDiffCellEvent) {
        handleEvent(event)
        eventProvider.sendEvent(event)
    }

    private fun handleEvent(event: GroupDiffCellEvent) {
        when (event) {
            is GroupDiffCellEvent.OnSelectionChanged -> {
                observableModel.value = observableModel.value.copy(
                    isChecked = event.isSelected
                )
            }
        }
    }
}