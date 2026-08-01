package com.ivanovsky.passnotes.util

import java.util.Date

object DateUtils {
    @JvmStatic
    fun anyLast(first: Date?, second: Date?): Date? {
        if (first == null) return second
        if (second == null) return first

        return if (first.after(second)) first else second
    }

    @JvmStatic
    fun anyLastTimestamp(first: Long?, second: Long?): Long? {
        if (first == null) return second
        if (second == null) return first

        return if (first > second) first else second
    }

    @JvmStatic
    fun anyLastTimestamp(first: Date?, second: Date?): Long? = anyLast(first, second)?.time
}