package com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel

import androidx.compose.runtime.Stable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEventProvider
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.model.ErrorPanelCellEvent
import com.ivanovsky.passnotes.presentation.core.compose.cells.model.ErrorPanelCellModel

@Stable
class ErrorPanelCellViewModel(
    override val model: ErrorPanelCellModel,
    private val eventProvider: CellEventProvider
) : CellViewModel {

    fun onReportButtonClicked() {
        eventProvider.sendEvent(
            ErrorPanelCellEvent.OnReportButtonClick(
                id = model.id,
                error = model.error
            )
        )
    }

    fun onCloseButtonClicked() {
        eventProvider.sendEvent(ErrorPanelCellEvent.OnCloseButtonClick(id = model.id))
    }

    fun onActionButtonClicked() {
        val actionId = model.actionId ?: return

        eventProvider.sendEvent(
            ErrorPanelCellEvent.OnActionButtonClick(
                id = model.id,
                actionId = actionId,
                error = model.error
            )
        )
    }
}