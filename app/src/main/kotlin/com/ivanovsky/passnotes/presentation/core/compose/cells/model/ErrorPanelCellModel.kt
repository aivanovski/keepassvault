package com.ivanovsky.passnotes.presentation.core.compose.cells.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEvent
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.IntCellId

@Immutable
data class ErrorPanelCellModel(
    override val id: IntCellId,
    val error: OperationError,
    val isCloseButtonVisible: Boolean,
    val background: Color?,
    val actionId: Int? = null,
    val actionButtonText: String? = null
) : CellModel

sealed interface ErrorPanelCellEvent : CellEvent {

    data class OnReportButtonClick(
        val id: IntCellId,
        val error: OperationError
    ) : ErrorPanelCellEvent

    data class OnCloseButtonClick(
        val id: IntCellId
    ) : ErrorPanelCellEvent

    data class OnActionButtonClick(
        val id: IntCellId,
        val actionId: Int,
        val error: OperationError
    ) : ErrorPanelCellEvent
}