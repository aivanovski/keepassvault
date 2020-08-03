package com.ivanovsky.passnotes.util

import java.text.SimpleDateFormat
import java.util.*

fun String.isDigitsOnly(): Boolean {
    return this.all { ch -> ch.isDigit() }
}

fun String.toUUID(): UUID? {
    if (length != 32 && length != 36) {
        return null
    }

    val isOnlyHexOrMinus = this.all { ch -> ch.isHex() || ch == '-' }
    if (!isOnlyHexOrMinus) {
        return null
    }

    if (contains("-") && length == 36) {
        return UUID.fromString(this)
    }

    val isOnlyHex = this.all { ch -> ch.isHex() }
    if (!isOnlyHex) {
        return null
    }

    val formattedUid = substring(0, 8) + "-" + substring(8, 12) + "-" + substring(12, 16) + "-" + substring(16, 20) + "-" + substring(20)

    return UUID.fromString(formattedUid)
}

private fun Char.isHex(): Boolean {
    return isDigit() || (toLowerCase() in 'a'..'f')
}

// parses string containing date "2020-02-18"
fun String.asDate(): Date {
    val date = DATE_FORMAT.parse(this) ?: throw IllegalStateException("Unable to parse date")

    val cal = Calendar.getInstance()
    cal.timeInMillis = date.time

    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    return cal.time
}

private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)