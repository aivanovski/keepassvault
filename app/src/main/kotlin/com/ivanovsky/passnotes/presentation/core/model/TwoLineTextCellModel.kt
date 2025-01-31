package com.ivanovsky.passnotes.presentation.core.model

data class TwoLineTextCellModel(
    override val id: String?,
    val title: String,
    val description: String
) : BaseCellModel(id)