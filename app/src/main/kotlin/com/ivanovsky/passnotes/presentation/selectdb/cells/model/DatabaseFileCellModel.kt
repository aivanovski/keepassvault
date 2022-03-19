package com.ivanovsky.passnotes.presentation.selectdb.cells.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import java.util.UUID

data class DatabaseFileCellModel(
    override val id: UUID,
    val name: String,
    val path: String,
    val status: String,
    val statusColor: Int,
    val isRemoveButtonVisible: Boolean,
    val isResolveButtonVisible: Boolean
) : BaseCellModel()