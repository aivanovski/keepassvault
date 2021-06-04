package com.ivanovsky.passnotes.util

import com.ivanovsky.passnotes.presentation.core.event.Event
import java.util.UUID

fun Event.takeInt(key: String): Int {
    return this[key] as Int
}

fun Event.takeUUID(key: String): UUID {
    return this[key] as UUID
}