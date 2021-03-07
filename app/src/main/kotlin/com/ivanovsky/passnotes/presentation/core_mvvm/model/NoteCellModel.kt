package com.ivanovsky.passnotes.presentation.core_mvvm.model

data class NoteCellModel(
    override val id: String,
    val title: String
) : BaseCellModel()