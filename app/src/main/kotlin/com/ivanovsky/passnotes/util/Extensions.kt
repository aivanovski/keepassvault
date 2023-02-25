package com.ivanovsky.passnotes.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun Date.formatAccordingLocale(locale: Locale): String {
    val format = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)

    return format.format(this)
}