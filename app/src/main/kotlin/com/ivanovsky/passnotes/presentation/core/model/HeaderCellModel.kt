package com.ivanovsky.passnotes.presentation.core.model

import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes

data class HeaderCellModel(
    override val id: String? = null,
    val title: String,
    val description: String,
    val isDescriptionVisible: Boolean,
    @DrawableRes
    val descriptionIconResId: Int?,
    val color: Int,
    val isBold: Boolean,
    val isClickable: Boolean,
    @DimenRes
    val paddingHorizontal: Int?
) : BaseCellModel()