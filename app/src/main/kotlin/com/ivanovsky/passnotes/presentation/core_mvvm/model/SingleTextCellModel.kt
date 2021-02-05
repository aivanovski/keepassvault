package com.ivanovsky.passnotes.presentation.core_mvvm.model

data class SingleTextCellModel(
    override val id: String?,
    val text: String
) : BaseCellModel()