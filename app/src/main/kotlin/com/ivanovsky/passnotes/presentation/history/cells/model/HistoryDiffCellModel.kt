package com.ivanovsky.passnotes.presentation.history.cells.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape

data class HistoryDiffCellModel(
    override val id: Int,
    val eventId: String,
    val name: String,
    val value: String,
    val event: String,
    val backgroundShape: RoundedShape,
    val backgroundColor: Int
) : BaseCellModel(id)