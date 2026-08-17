package com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel

import androidx.compose.runtime.Immutable
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.model.CenteredTextCellModel

@Immutable
class CenteredTextCellViewModel(
    override val model: CenteredTextCellModel
) : CellViewModel