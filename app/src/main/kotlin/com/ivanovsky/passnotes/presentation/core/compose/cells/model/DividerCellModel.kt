package com.ivanovsky.passnotes.presentation.core.compose.cells.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.IntCellId

@Immutable
data class DividerCellModel(
    override val id: IntCellId,
    val color: Color,
    val paddingStart: Dp,
    val paddingEnd: Dp
) : CellModel