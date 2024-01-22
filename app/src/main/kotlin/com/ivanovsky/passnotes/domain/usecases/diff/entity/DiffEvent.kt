package com.ivanovsky.passnotes.domain.usecases.diff.entity

import java.util.UUID

sealed class DiffEvent<T : Any> {

    data class Insert<T : Any>(
        val parentUuid: UUID?,
        val entity: T
    ) : DiffEvent<T>()

    data class Delete<T : Any>(
        val parentUuid: UUID?,
        val entity: T
    ) : DiffEvent<T>()

    data class Update<T : Any>(
        val oldParentUuid: UUID?,
        val newParentUuid: UUID?,
        val oldEntity: T,
        val newEntity: T
    ) : DiffEvent<T>()
}