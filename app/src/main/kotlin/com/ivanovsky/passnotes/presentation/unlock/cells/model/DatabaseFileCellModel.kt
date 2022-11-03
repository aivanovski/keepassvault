package com.ivanovsky.passnotes.presentation.unlock.cells.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

data class DatabaseFileCellModel(
    override val id: Int,
    val filename: String,
    val path: String,
    val status: String,
    val statusColor: Int,
    val isStatusVisible: Boolean,
    val isSelected: Boolean
) : BaseCellModel(id)