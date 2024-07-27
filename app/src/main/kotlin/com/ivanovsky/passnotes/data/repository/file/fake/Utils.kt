package com.ivanovsky.passnotes.data.repository.file.fake

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale

private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val DATE_AND_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

fun parseDate(str: String): Long {
    return DATE_FORMAT.parse(str)?.time ?: throw IllegalArgumentException()
}

fun parseDateAndTime(str: String): Long {
    return DATE_AND_TIME_FORMAT.parse(str)?.time ?: throw IllegalArgumentException()
}

fun Long.toInstant(): Instant {
    return Instant.ofEpochMilli(this)
}

fun Map<String, String>.plus(
    vararg pairs: Pair<String, String>
): Map<String, String> {
    return this.plus(pairs.toMap())
}