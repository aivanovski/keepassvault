package com.ivanovsky.passnotes.presentation.core.model

import androidx.annotation.DrawableRes

data class FileCellModel(
    override val id: String,
    @DrawableRes val iconResId: Int,
    val title: String,
    val description: String,
    val isSelected: Boolean
) : BaseCellModel()