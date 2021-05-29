package com.ivanovsky.passnotes.presentation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.entity.ServiceState
import com.ivanovsky.passnotes.extensions.getNotificationManager
import com.ivanovsky.passnotes.presentation.MainActivity
import java.util.concurrent.atomic.AtomicReference

class DatabaseLockService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(LOCK_NOTIFICATION_ID, createNotification())
        state.set(ServiceState.STARTED)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        state.set(ServiceState.STOPPED)
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getChannelId(),
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }

            getNotificationManager().createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(this, 0, createContentIntent(), 0)
        val actionIntent = PendingIntent.getBroadcast(this, 0, createLockButtonIntent(), 0)

        val builder =  NotificationCompat.Builder(this, getChannelId())
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_lock_open_white_24dp)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_lock_grey_600_24dp, getString(R.string.lock_database), actionIntent)

        return builder.build()
    }

    private fun createContentIntent(): Intent {
        return Intent(this, MainActivity::class.java)
    }

    private fun createLockButtonIntent(): Intent {
        return Intent(this, DatabaseLockBroadcastReceiver::class.java)
    }

    private fun getChannelId(): String {
        return applicationContext.packageName + "_notification_channel"
    }

    companion object {

        private const val LOCK_NOTIFICATION_ID = 1

        private val state = AtomicReference(ServiceState.STOPPED)

        fun getCurrentState(): ServiceState = state.get()

        fun start(context: Context) {
            state.set(ServiceState.AWAITING_FOR_START)
            context.startService(createIntent(context))
        }

        fun stop(context: Context) {
            context.stopService(createIntent(context))
        }

        private fun createIntent(context: Context): Intent {
            return Intent(context, DatabaseLockService::class.java)
        }
    }
}