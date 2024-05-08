package com.ivanovsky.passnotes.data.repository.file.databaseDsl

import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.util.UUID

data class EntryEntity(
    val title: String,
    val uuid: UUID = UUID(1000L, title.hashCode().toLong()),
    val username: String = EMPTY,
    val password: String = EMPTY,
    val url: String = EMPTY,
    val notes: String = EMPTY,
    val created: Long = System.currentTimeMillis(),
    val modified: Long = System.currentTimeMillis(),
    val expires: Long? = null,
    val custom: Map<String, String> = emptyMap()
)