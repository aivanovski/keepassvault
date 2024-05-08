package com.ivanovsky.passnotes.domain.entity

import java.util.Calendar

class TimestampBuilder(
    initTimestamp: Timestamp
) {

    private val calendar = Calendar.getInstance()
        .apply {
            timeInMillis = initTimestamp.timeInMillis
        }

    fun shiftMonth(numberOfMonths: Int): TimestampBuilder {
        calendar.add(Calendar.MONTH, numberOfMonths)
        return this
    }

    fun setSecond(second: Int): TimestampBuilder {
        calendar.set(Calendar.SECOND, second)
        return this
    }

    fun setMinute(minute: Int): TimestampBuilder {
        calendar.set(Calendar.MINUTE, minute)
        return this
    }

    fun build(): Timestamp {
        return Timestamp(calendar.timeInMillis)
    }
}