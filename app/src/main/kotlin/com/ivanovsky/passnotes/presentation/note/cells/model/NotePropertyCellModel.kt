package com.ivanovsky.passnotes.presentation.note.cells.model

import androidx.annotation.DrawableRes
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape

data class NotePropertyCellModel(
    override val id: String,
    val name: String,
    val value: String,
    val backgroundShape: RoundedShape,
    val backgroundColor: Int,
    val isVisibilityButtonVisible: Boolean,
    val isValueProtected: Boolean,
    @DrawableRes
    val iconResId: Int?
) : BaseCellModel()