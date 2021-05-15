package com.ivanovsky.passnotes.presentation.core_mvvm.event

import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class Event : LinkedHashMap<String, Any>() {

    fun getString(key: String): String? {
        return this[key] as? String
    }

    companion object {

        fun Pair<String, Any?>.toEvent(): Event {
            return Event().apply {
                put(first, second ?: EMPTY)
            }
        }
    }
}