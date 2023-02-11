package com.ivanovsky.passnotes.presentation.note_editor.cells.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape

data class AttachmentCellModel(
    override val id: String,
    val name: String,
    val size: String
) : BaseCellModel(id)