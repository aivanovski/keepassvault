package com.ivanovsky.passnotes.presentation.noteEditor.cells.model

import com.ivanovsky.passnotes.domain.entity.Timestamp
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

data class ExpirationCellModel(
    override val id: String,
    val isEnabled: Boolean,
    val timestamp: Timestamp
) : BaseCellModel(id)