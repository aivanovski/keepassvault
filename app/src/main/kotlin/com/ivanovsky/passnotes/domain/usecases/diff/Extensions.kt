package com.ivanovsky.passnotes.domain.usecases.diff

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseElement
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEvent
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEventType

fun DiffEvent<*>.getType(): DiffEventType {
    return when (this) {
        is DiffEvent.Insert -> DiffEventType.INSERT
        is DiffEvent.Delete -> DiffEventType.DELETE
        is DiffEvent.Update -> DiffEventType.UPDATE
    }
}

fun DiffEventType.getBackgroundColor(resourceProvider: ResourceProvider): Int {
    return when (this) {
        DiffEventType.INSERT -> resourceProvider.getAttributeColor(R.attr.kpDiffInsertColor)
        DiffEventType.DELETE -> resourceProvider.getAttributeColor(R.attr.kpDiffDeleteColor)
        DiffEventType.UPDATE -> resourceProvider.getAttributeColor(R.attr.kpDiffUpdateColor)
    }
}

fun DiffEventType.getCharacter(): Char {
    return when (this) {
        DiffEventType.INSERT -> '+'
        DiffEventType.DELETE -> '-'
        DiffEventType.UPDATE -> '~'
    }
}

fun <T : EncryptedDatabaseElement> DiffEvent<T>.getEntity(): T {
    return when (this) {
        is DiffEvent.Insert -> entity
        is DiffEvent.Delete -> entity
        is DiffEvent.Update -> newEntity
    }
}