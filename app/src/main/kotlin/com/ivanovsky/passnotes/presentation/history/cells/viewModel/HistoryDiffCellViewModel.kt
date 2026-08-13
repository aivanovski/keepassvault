package com.ivanovsky.passnotes.presentation.history.cells.viewModel

import androidx.compose.runtime.Immutable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEventProvider
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryDiffCellEvent
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryDiffCellModel

@Immutable
class HistoryDiffCellViewModel(
    override val model: HistoryDiffCellModel,
    private val eventProvider: CellEventProvider
) : CellViewModel {

    fun onClicked() {
        eventProvider.sendEvent(HistoryDiffCellEvent.OnClick(model.eventId))
    }
}