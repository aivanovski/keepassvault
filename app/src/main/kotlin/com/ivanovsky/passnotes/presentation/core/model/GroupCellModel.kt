package com.ivanovsky.passnotes.presentation.core.model

import com.ivanovsky.passnotes.data.entity.Group

data class GroupCellModel(
    override val id: String,
    val group: Group,
    val noteCount: Int,
    val childGroupCount: Int
) : BaseCellModel()