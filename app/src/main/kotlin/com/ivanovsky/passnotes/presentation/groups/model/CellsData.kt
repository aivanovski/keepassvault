package com.ivanovsky.passnotes.presentation.groups.model

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel

data class CellsData(
    val isResetScroll: Boolean,
    val viewModels: List<BaseCellViewModel>
)