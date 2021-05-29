package com.ivanovsky.passnotes.extensions

import android.app.NotificationManager
import android.app.Service
import android.content.Context

fun Context.getNotificationManager(): NotificationManager {
    return getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
}