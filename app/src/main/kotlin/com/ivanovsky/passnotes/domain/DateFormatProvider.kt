package com.ivanovsky.passnotes.domain

import android.content.Context
import java.text.DateFormat

class DateFormatProvider(
    private val content: Context,
    private val localeProvider: LocaleProvider
) {

    fun getShortDateFormat(): DateFormat {
        return DateFormat.getDateInstance(DateFormat.MEDIUM, localeProvider.getSystemLocale())
    }

    fun getLongDateFormat(): DateFormat {
        return DateFormat.getDateInstance(DateFormat.LONG, localeProvider.getSystemLocale())
    }

    fun getTimeFormat(): DateFormat {
        return DateFormat.getTimeInstance()
    }

    fun is24HourFormat(): Boolean {
        return android.text.format.DateFormat.is24HourFormat(content)
    }
}