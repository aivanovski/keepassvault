package com.ivanovsky.passnotes.domain

import android.content.Context
import com.ivanovsky.passnotes.util.LocaleUtils
import java.util.Locale

class LocaleProvider(
    private val context: Context
) {

    fun getSystemLocale(): Locale {
        return LocaleUtils.getSystemLocale(context)
    }
}