package com.ivanovsky.passnotes.presentation.noteEditor.cells.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

data class AttachmentCellModel(
    override val id: String,
    val name: String,
    val size: String
) : BaseCellModel(id)