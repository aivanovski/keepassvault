package com.ivanovsky.passnotes.presentation.core.compose.cells.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.SpaceCellViewModel

@Composable
fun SpaceCell(viewModel: SpaceCellViewModel) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(viewModel.model.height)
    )
}