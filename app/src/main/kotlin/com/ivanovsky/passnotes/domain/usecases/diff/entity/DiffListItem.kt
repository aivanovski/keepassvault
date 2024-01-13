package com.ivanovsky.passnotes.domain.usecases.diff.entity

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseElement

sealed class DiffListItem {

    data class Parent(
        val entity: EncryptedDatabaseElement?,
    ) : DiffListItem()

    data class Event(
        val event: DiffEvent<EncryptedDatabaseElement>,
    ) : DiffListItem()
}