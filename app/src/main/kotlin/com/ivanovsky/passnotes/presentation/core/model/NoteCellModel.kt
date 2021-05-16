package com.ivanovsky.passnotes.presentation.core.model

data class NoteCellModel(
    override val id: String,
    val title: String
) : BaseCellModel()