package com.ivanovsky.passnotes.presentation.core.model

import androidx.annotation.ColorInt

data class MessageCellModel(
    val text: String,
    @ColorInt
    val backgroundColor: Int,
    val isVisible: Boolean
) : BaseCellModel()