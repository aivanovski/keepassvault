package com.ivanovsky.passnotes.util

import android.content.Context
import android.os.Build
import java.util.Locale

object LocaleUtils {
    @JvmStatic
    @Suppress("DEPRECATION")
    fun getSystemLocale(context: Context): Locale {
        val configuration = context.resources.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales[0]
        } else {
            configuration.locale
        }
    }
}