package com.ivanovsky.passnotes.extensions

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.view.ContextThemeWrapper as DefaultContextThemeWrapper
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper

fun Context.getNotificationManager(): NotificationManager {
    return getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
}

fun Context.cloneWithTheme(@StyleRes themeResId: Int): Context {
    return ContextThemeWrapper(this, themeResId)
}

fun Context.getOrUnwrapActivity(): AppCompatActivity {
    return when (this) {
        is AppCompatActivity -> this
        is ContextThemeWrapper -> baseContext.getOrUnwrapActivity()
        is DefaultContextThemeWrapper -> baseContext.getOrUnwrapActivity()
        else -> throw IllegalStateException("Unable to unwrap Activity from: $this")
    }
}