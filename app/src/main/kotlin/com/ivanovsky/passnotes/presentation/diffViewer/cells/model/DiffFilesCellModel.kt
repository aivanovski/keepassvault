package com.ivanovsky.passnotes.presentation.diffViewer.cells.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

data class DiffFilesCellModel(
    override val id: Int,
    val leftTitle: String,
    val leftTime: String,
    val rightTitle: String,
    val rightTime: String
) : BaseCellModel()