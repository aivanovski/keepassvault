package com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel

import androidx.compose.runtime.Stable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEventProvider
import com.ivanovsky.passnotes.presentation.core.compose.cells.MutableCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.DiffHeaderCellEvent
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.DiffHeaderCellModel

@Stable
class DiffHeaderCellViewModel(
    initialModel: DiffHeaderCellModel,
    private val eventProvider: CellEventProvider
) : MutableCellViewModel<DiffHeaderCellModel>(initialModel) {

    fun onCheckedChanged(isChecked: Boolean) {
        observableModel.value = observableModel.value.copy(isChecked = isChecked)

        eventProvider.sendEvent(
            DiffHeaderCellEvent.OnSelectionChanged(
                id = observableModel.value.id,
                isSelected = isChecked
            )
        )
    }
}