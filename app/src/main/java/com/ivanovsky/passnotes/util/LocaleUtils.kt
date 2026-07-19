package com.ivanovsky.passnotes.util

import android.content.Context
import java.util.Locale

object LocaleUtils {

    @JvmStatic
    fun getSystemLocale(context: Context): Locale {
        return context.resources.configuration.locale
    }
}