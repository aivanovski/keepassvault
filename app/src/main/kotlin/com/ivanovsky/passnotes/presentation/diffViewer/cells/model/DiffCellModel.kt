package com.ivanovsky.passnotes.presentation.diffViewer.cells.model

import androidx.annotation.DrawableRes
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

class DiffCellModel(
    override val id: Int,
    val eventId: Int,
    val backgroundColor: Int,
    @DrawableRes
    val iconResId: Int?,
    val text: String
) : BaseCellModel(id)