package com.ivanovsky.passnotes.presentation.diffViewer.cells.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

class DiffHeaderCellModel(
    override val id: Int,
    val backgroundColor: Int,
    val text: String
) : BaseCellModel(id)