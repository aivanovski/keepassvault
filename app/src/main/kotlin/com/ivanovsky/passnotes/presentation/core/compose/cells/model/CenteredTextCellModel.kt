package com.ivanovsky.passnotes.presentation.core.compose.cells.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.IntCellId

@Immutable
data class CenteredTextCellModel(
    override val id: IntCellId,
    val title: String,
    val height: Dp
) : CellModel