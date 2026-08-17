package com.ivanovsky.passnotes.presentation.core.compose.cells.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.ivanovsky.passnotes.presentation.core.compose.ElementMargin
import com.ivanovsky.passnotes.presentation.core.compose.cells.model.SpaceCellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.toId
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.SpaceCellViewModel

@Composable
fun SpaceCell(viewModel: SpaceCellViewModel) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(viewModel.model.height)
    )
}

@Composable
fun newSpaceCell(height: Dp = ElementMargin) =
    SpaceCellViewModel(
        model = SpaceCellModel(
            id = 1.toId(),
            height = height
        )
    )