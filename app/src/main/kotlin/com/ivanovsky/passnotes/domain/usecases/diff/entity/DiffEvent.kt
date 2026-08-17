package com.ivanovsky.passnotes.domain.usecases.diff.entity

import java.util.UUID

sealed class DiffEvent<out T : Any>(val type: DiffEventType) {

    data class Insert<T : Any>(
        val parentUuid: UUID?,
        val entity: T
    ) : DiffEvent<T>(DiffEventType.INSERT)

    data class Delete<T : Any>(
        val parentUuid: UUID?,
        val entity: T
    ) : DiffEvent<T>(DiffEventType.DELETE)

    data class Update<T : Any>(
        val oldParentUuid: UUID?,
        val newParentUuid: UUID?,
        val oldEntity: T,
        val newEntity: T
    ) : DiffEvent<T>(DiffEventType.UPDATE)
}