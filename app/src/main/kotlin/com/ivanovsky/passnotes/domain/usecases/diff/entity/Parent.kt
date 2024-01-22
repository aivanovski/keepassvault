package com.ivanovsky.passnotes.domain.usecases.diff.entity

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseElement
import java.util.UUID

data class Parent(
    val uuid: UUID,
    val entity: EncryptedDatabaseElement?
)