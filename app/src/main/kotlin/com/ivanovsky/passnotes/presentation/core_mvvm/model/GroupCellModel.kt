package com.ivanovsky.passnotes.presentation.core_mvvm.model

data class GroupCellModel(
    override val id: String,
    val title: String,
    val countText: String
) : BaseCellModel()