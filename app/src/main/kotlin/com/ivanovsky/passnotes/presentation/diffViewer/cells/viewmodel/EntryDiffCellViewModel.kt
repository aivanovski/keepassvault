package com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel

import androidx.compose.runtime.Stable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEventProvider
import com.ivanovsky.passnotes.presentation.core.compose.cells.MutableCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.EntryDiffCellEvent
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.EntryDiffCellModel

@Stable
class EntryDiffCellViewModel(
    initialModel: EntryDiffCellModel,
    private val eventProvider: CellEventProvider
) : MutableCellViewModel<EntryDiffCellModel>(initialModel) {

    fun sendEvent(event: EntryDiffCellEvent) {
        handleEvent(event)
        eventProvider.sendEvent(event)
    }

    private fun handleEvent(event: EntryDiffCellEvent) {
        when (event) {
            is EntryDiffCellEvent.OnExpandedChanged -> {
                observableModel.value = observableModel.value.copy(
                    isExpanded = event.isExpanded
                )
            }

            is EntryDiffCellEvent.OnSelectionChanged -> {
                observableModel.value = observableModel.value.copy(
                    isChecked = event.isSelected
                )
            }
        }
    }
}