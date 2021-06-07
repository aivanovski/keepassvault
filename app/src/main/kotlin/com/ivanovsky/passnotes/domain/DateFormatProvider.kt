package com.ivanovsky.passnotes.domain

import java.text.DateFormat

class DateFormatProvider(
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
}