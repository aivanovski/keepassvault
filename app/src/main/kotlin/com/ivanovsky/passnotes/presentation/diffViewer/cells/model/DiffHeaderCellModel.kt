package com.ivanovsky.passnotes.presentation.diffViewer.cells.model

import androidx.compose.runtime.Immutable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEvent
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.IntCellId

@Immutable
data class DiffHeaderCellModel(
    override val id: IntCellId,
    val title: String,
    val description: String,
    val isCheckable: Boolean,
    val isChecked: Boolean
) : CellModel

sealed interface DiffHeaderCellEvent : CellEvent {

    data class OnSelectionChanged(
        val id: IntCellId,
        val isSelected: Boolean
    ) : DiffHeaderCellEvent
}