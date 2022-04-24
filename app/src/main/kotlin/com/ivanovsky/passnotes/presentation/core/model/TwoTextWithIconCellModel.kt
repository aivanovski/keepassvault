package com.ivanovsky.passnotes.presentation.core.model

import androidx.annotation.DrawableRes

data class TwoTextWithIconCellModel(
    override val id: String?,
    val title: String,
    val description: String,
    @DrawableRes
    val iconResId: Int
) : BaseCellModel()