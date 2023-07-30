package com.ivanovsky.passnotes.util

import android.content.Context
import android.content.res.Configuration

object ThemeUtils {

    fun isNightMode(context: Context): Boolean {
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }
}