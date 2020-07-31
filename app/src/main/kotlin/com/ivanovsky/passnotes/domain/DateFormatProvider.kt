package com.ivanovsky.passnotes.domain

import java.text.DateFormat

class DateFormatProvider(
    private val localeProvider: LocaleProvider
) {

    fun getShortDateFormat(): DateFormat {
        return DateFormat.getDateInstance(DateFormat.MEDIUM, localeProvider.getSystemLocale())
    }
}