package com.ivanovsky.passnotes.presentation.core.model

data class OneLineTextCellModel(
    override val id: String?,
    val text: String
) : BaseCellModel()