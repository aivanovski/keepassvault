package com.ivanovsky.passnotes.presentation.diffViewer.model

import com.ivanovsky.passnotes.data.repository.keepass.kotpass.KotpassDatabase
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffListItem

data class MergeData(
    val base: KotpassDatabase,
    val local: KotpassDatabase,
    val remote: KotpassDatabase,
    val localDiff: List<DiffListItem>,
    val remoteDiff: List<DiffListItem>
)