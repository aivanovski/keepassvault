package com.ivanovsky.passnotes.data.entity

import java.util.UUID

data class GroupEntity(
    val uid: UUID? = null,
    val parentUid: UUID? = null,
    val title: String
)