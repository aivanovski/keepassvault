package com.ivanovsky.passnotes.presentation.history.cells.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEvent
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellModel

@Immutable
data class HistoryHeaderCellModel(
    override val id: Int,
    val itemId: Int,
    val title: String,
    val description: String,
    @DrawableRes
    val descriptionIcon: Int
) : CellModel

sealed interface HistoryHeaderCellEvent : CellEvent {
    data class OnClick(val itemId: Int) : HistoryHeaderCellEvent
}