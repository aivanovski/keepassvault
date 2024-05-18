package com.ivanovsky.passnotes.presentation.history.cells

import androidx.compose.runtime.Composable
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.DividerCell
import com.ivanovsky.passnotes.presentation.core.viewmodel.DividerCellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.ui.HistoryDiffCell
import com.ivanovsky.passnotes.presentation.history.cells.ui.HistoryDiffPlaceholderCell
import com.ivanovsky.passnotes.presentation.history.cells.ui.HistoryHeaderCell
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryDiffCellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryDiffPlaceholderCellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryHeaderCellViewModel

class CellFactory {

    @Composable
    fun createCell(viewModel: BaseCellViewModel) {
        when (viewModel) {
            is HistoryHeaderCellViewModel -> HistoryHeaderCell(viewModel)
            is HistoryDiffCellViewModel -> HistoryDiffCell(viewModel)
            is HistoryDiffPlaceholderCellViewModel -> HistoryDiffPlaceholderCell(viewModel)
            is DividerCellViewModel -> DividerCell(viewModel)
        }
    }
}