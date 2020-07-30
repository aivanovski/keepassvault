package com.ivanovsky.passnotes.util

import android.content.Context
import java.text.DateFormat
import java.util.*

fun Date.formatAccordingLocale(locale: Locale): String {
    val format = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)

    return format.format(this)
}