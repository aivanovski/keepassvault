package com.ivanovsky.passnotes.presentation.core.compose.cells

import androidx.compose.runtime.Stable

/**
 * The interface to represent Cell ViewModel for compose UI
 */
@Stable
interface CellViewModel {
    val model: CellModel
}