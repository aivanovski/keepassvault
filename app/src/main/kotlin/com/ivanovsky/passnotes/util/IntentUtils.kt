package com.ivanovsky.passnotes.util

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build

object IntentUtils {

    fun newOpenUrlIntent(url: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
           data = Uri.parse(url)
        }
    }

    fun newOpenFileIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*";
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }

    fun newCreateFileIntent(filename: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_TITLE, filename)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }

    fun defaultPendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    fun immutablePendingIntentFlags(vararg flags: Int): Int {
        return if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.FLAG_IMMUTABLE + flags.sum()
        } else {
            flags.sum()
        }
    }
}