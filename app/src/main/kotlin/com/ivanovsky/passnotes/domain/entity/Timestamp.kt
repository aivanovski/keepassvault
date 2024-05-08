package com.ivanovsky.passnotes.domain.entity

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Timestamp(
    val timeInMillis: Long
) {
    override fun toString(): String {
        return "${Timestamp::class.simpleName}(${DATE_FORMAT.format(Date(timeInMillis))})"
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        fun currentTimestamp(): Timestamp {
            return Timestamp(timeInMillis = System.currentTimeMillis())
        }
    }
}