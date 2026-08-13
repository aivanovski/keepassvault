package com.ivanovsky.passnotes.domain.usecases.diff.entity

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import java.util.UUID

data class Parent(
    val uuid: UUID,
    val entity: EncryptedDatabaseEntry?
)