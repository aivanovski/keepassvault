package com.ivanovsky.passnotes.presentation.history.cells.viewModel

import androidx.compose.runtime.Immutable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEventProvider
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryHeaderCellEvent
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryHeaderCellModel

@Immutable
class HistoryHeaderCellViewModel(
    override val model: HistoryHeaderCellModel,
    private val eventProvider: CellEventProvider
) : CellViewModel {

    fun onClicked() {
        eventProvider.sendEvent(HistoryHeaderCellEvent.OnClick(model.itemId))
    }
}