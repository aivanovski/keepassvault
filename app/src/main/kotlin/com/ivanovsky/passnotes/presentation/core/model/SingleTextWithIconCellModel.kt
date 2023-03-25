package com.ivanovsky.passnotes.presentation.core.model

import androidx.annotation.DrawableRes

data class SingleTextWithIconCellModel(
    override val id: Int,
    val title: String,
    @DrawableRes
    val iconResId: Int
) : BaseCellModel(id)