package com.ivanovsky.passnotes.util

import android.content.Intent
import android.os.Build
import android.os.Parcelable

fun <T : Parcelable> Intent.getParcelable(key: String, type: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= 33) {
        this.getParcelableExtra(key, type)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}