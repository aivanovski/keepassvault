package com.ivanovsky.passnotes.presentation.diffViewer.cells.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEvent
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.IntCellId
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape

@Immutable
data class EntryDiffCellModel(
    override val id: IntCellId,
    val eventType: EventType,
    val title: String,
    val path: String,
    val fields: List<Field>,
    val icon: ImageVector,
    val iconBackgroundTint: Color,
    val accentTextColor: Color,
    val isCheckable: Boolean,
    val isChecked: Boolean,
    val isExpanded: Boolean
) : CellModel

enum class EventType {
    INSERT,
    DELETE,
    UPDATE
}

@Immutable
data class Field(
    val eventType: EventType,
    val name: String,
    val value: FieldValue,
    val backgroundShape: RoundedShape
)

@Immutable
sealed interface FieldValue {
    data class Value(
        val value: String
    ) : FieldValue

    data class Update(
        val oldValue: String,
        val newValue: String
    ) : FieldValue
}

sealed interface EntryDiffCellEvent : CellEvent {

    data class OnSelectionChanged(
        val id: IntCellId,
        val isSelected: Boolean
    ) : EntryDiffCellEvent

    data class OnExpandedChanged(
        val id: IntCellId,
        val isExpanded: Boolean
    ) : EntryDiffCellEvent
}