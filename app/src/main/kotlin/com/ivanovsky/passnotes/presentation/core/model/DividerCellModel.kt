package com.ivanovsky.passnotes.presentation.core.model

import androidx.annotation.DimenRes

data class DividerCellModel(
    val color: Int,
    @DimenRes
    val paddingStart: Int?,
    @DimenRes
    val paddingEnd: Int?
) : BaseCellModel()