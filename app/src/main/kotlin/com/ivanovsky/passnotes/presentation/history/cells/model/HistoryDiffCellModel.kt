package com.ivanovsky.passnotes.presentation.history.cells.model

import androidx.compose.runtime.Immutable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEvent
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellModel
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape

@Immutable
data class HistoryDiffCellModel(
    override val id: Int,
    val eventId: String,
    val name: String,
    val value: String,
    val event: String,
    val backgroundShape: RoundedShape,
    val backgroundColor: Int
) : CellModel

sealed interface HistoryDiffCellEvent : CellEvent {
    data class OnClick(val eventId: String) : HistoryDiffCellEvent
}