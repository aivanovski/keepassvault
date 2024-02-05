package com.ivanovsky.passnotes.domain.usecases.history.entity

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEvent
import java.util.Date

data class HistoryDiffItem(
    val date: Date,
    val diff: List<DiffEvent<Property>>
)