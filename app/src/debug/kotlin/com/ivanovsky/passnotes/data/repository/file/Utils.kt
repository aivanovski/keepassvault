package com.ivanovsky.passnotes.data.repository.file

import java.text.SimpleDateFormat
import java.util.Locale

private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

fun parseDate(str: String): Long {
    return DATE_FORMAT.parse(str)?.time ?: throw IllegalArgumentException()
}