package com.ivanovsky.passnotes.presentation.diffViewer.model

import com.ivanovsky.passnotes.data.repository.keepass.kotpass.KotpassDatabase
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffListItem

data class CompareData(
    val left: KotpassDatabase,
    val right: KotpassDatabase,
    val diff: List<DiffListItem>
)