package com.ivanovsky.passnotes.presentation.core.model

import androidx.annotation.DimenRes

data class HeaderCellModel(
    override val id: String? = null,
    val title: String,
    val color: Int,
    val isBold: Boolean,
    @DimenRes
    val paddingHorizontal: Int?
) : BaseCellModel()