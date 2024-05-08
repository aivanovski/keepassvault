package com.ivanovsky.passnotes.util

import com.ivanovsky.passnotes.domain.entity.DateData
import com.ivanovsky.passnotes.domain.entity.TimeData
import com.ivanovsky.passnotes.domain.entity.Timestamp
import java.util.Calendar
import java.util.Date

object TimeUtils {

    private val EMPTY_TIME_DATA = TimeData(0, 0, 0)

    fun Date.toTimestamp(): Timestamp {
        return Timestamp(timeInMillis = time)
    }

    fun Timestamp.toJavaDate(): Date {
        return Date(timeInMillis)
    }

    fun Timestamp.toDate(): DateData {
        val cal = Calendar.getInstance()

        cal.timeInMillis = timeInMillis

        return DateData(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH),
            day = cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun Timestamp.toTime(): TimeData {
        val cal = Calendar.getInstance()

        cal.timeInMillis = timeInMillis

        return TimeData(
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
            second = cal.get(Calendar.SECOND)
        )
    }

    fun Long.toTimestamp(): Timestamp {
        return Timestamp(timeInMillis = this)
    }

    fun combine(date: DateData, time: TimeData): Timestamp {
        val cal = Calendar.getInstance()

        cal.set(Calendar.YEAR, date.year)
        cal.set(Calendar.MONTH, date.month)
        cal.set(Calendar.DAY_OF_MONTH, date.day)

        cal.set(Calendar.HOUR_OF_DAY, time.hour)
        cal.set(Calendar.MINUTE, time.minute)
        cal.set(Calendar.SECOND, time.second)
        cal.set(Calendar.MILLISECOND, 0)

        return cal.timeInMillis.toTimestamp()
    }

    fun DateData.toTimestamp(): Timestamp {
        return combine(this, EMPTY_TIME_DATA)
    }
}