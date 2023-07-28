package com.ivanovsky.passnotes.extensions

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper

fun Context.getNotificationManager(): NotificationManager {
    return getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
}

fun Context.cloneWithTheme(@StyleRes themeResId: Int): Context {
    return ContextThemeWrapper(this, themeResId)
}