package com.ivanovsky.passnotes.presentation.history.factory

import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.DividerCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.DividerCellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryDiffCellModel
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryDiffPlaceholderCellModel
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryHeaderCellModel
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryDiffCellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryDiffPlaceholderCellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryHeaderCellViewModel

class HistoryCellViewModelFactory(
    private val resourceProvider: ResourceProvider
) : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is HistoryHeaderCellModel -> HistoryHeaderCellViewModel(model, eventProvider)
            is HistoryDiffCellModel -> HistoryDiffCellViewModel(model, eventProvider)
            is HistoryDiffPlaceholderCellModel -> HistoryDiffPlaceholderCellViewModel(model)
            is DividerCellModel -> DividerCellViewModel(model, resourceProvider)
            else -> throwUnsupportedModelException(model)
        }
    }
}