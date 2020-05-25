package com.ivanovsky.passnotes.util

import android.content.Context
import java.text.DateFormat
import java.util.*

fun Date.formatAccordingSystemLocale(context: Context): String {
    val locale = LocaleUtils.getSystemLocale(context)

    val format = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)

    return format.format(this)
}