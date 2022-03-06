package com.ivanovsky.passnotes.presentation.core.model

data class ProtectedNotePropertyCellModel(
    override val id: String,
    val name: String,
    val value: String,
    val isCopyButtonVisible: Boolean
) : BaseCellModel()