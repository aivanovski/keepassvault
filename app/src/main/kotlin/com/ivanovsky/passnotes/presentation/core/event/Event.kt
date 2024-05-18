package com.ivanovsky.passnotes.presentation.core.event

import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class Event : LinkedHashMap<String, Any>() {

    fun getString(key: String): String? {
        return this[key] as? String
    }

    fun getInt(key: String): Int? {
        return this[key] as? Int
    }

    fun key(): String? {
        return this.keys.firstOrNull()
    }

    companion object {

        fun Pair<String, Any?>.toEvent(): Event {
            return Event().apply {
                put(first, second ?: EMPTY)
            }
        }
    }
}