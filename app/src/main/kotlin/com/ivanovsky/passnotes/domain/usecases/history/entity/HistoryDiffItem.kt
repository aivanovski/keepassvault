package com.ivanovsky.passnotes.domain.usecases.history.entity

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEvent

data class HistoryDiffItem(
    val oldNote: Note,
    val newNote: Note,
    val diffEvents: List<DiffEvent<Property>>
)