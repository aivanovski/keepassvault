package com.ivanovsky.passnotes.presentation.core.model

import com.ivanovsky.passnotes.data.entity.Note

data class NoteCellModel(
    override val id: String,
    val note: Note
) : BaseCellModel()