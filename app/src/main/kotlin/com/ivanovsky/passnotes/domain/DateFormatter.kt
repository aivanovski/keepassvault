package com.ivanovsky.passnotes.domain

import com.ivanovsky.passnotes.util.StringUtils.SPACE
import java.util.Date

class DateFormatter(
    private val dateFormatProvider: DateFormatProvider,
) {

    fun formatShortDate(time: Date): String {
        return dateFormatProvider.getShortDateFormat().format(time)
    }

    fun formatDateAndTime(time: Date): String {
        val dateFormat = dateFormatProvider.getShortDateFormat()
        val timeFormat = dateFormatProvider.getTimeFormat()

        return dateFormat.format(time) + SPACE + timeFormat.format(time)
    }
}