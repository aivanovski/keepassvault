package com.ivanovsky.passnotes.presentation.history.cells.model

import androidx.annotation.DrawableRes
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

data class HistoryHeaderCellModel(
    override val id: Int,
    val itemId: Int,
    val title: String,
    val description: String,
    @DrawableRes
    val descriptionIcon: Int
) : BaseCellModel(id)