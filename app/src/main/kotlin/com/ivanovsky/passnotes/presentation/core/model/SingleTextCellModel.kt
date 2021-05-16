package com.ivanovsky.passnotes.presentation.core.model

data class SingleTextCellModel(
    override val id: String?,
    val text: String
) : BaseCellModel()