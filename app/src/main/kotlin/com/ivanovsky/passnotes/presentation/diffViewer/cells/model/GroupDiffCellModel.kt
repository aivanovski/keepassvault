package com.ivanovsky.passnotes.presentation.diffViewer.cells.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEvent
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.IntCellId

@Immutable
data class GroupDiffCellModel(
    override val id: IntCellId,
    val title: String,
    val description: String,
    val path: String,
    val chipBackgroundTint: Color,
    val isCheckable: Boolean,
    val isChecked: Boolean
) : CellModel

sealed interface GroupDiffCellEvent : CellEvent {
    data class OnSelectionChanged(
        val id: IntCellId,
        val isSelected: Boolean
    ) : GroupDiffCellEvent
}