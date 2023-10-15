package com.ivanovsky.passnotes.data.repository.file.databaseDsl

import java.util.UUID

data class GroupEntity(
    val title: String,
    val uuid: UUID = UUID(1L, title.hashCode().toLong())
)