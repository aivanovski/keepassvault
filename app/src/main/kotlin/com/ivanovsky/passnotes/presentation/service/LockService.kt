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
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.domain.interactor.service.LockServiceInteractor
import com.ivanovsky.passnotes.presentation.service.model.ServiceState
import com.ivanovsky.passnotes.extensions.getNotificationManager
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.main.MainActivity
import com.ivanovsky.passnotes.presentation.service.model.LockServiceCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

class LockService : Service(), ObserverBus.DatabaseStatusObserver {

    private val interactor: LockServiceInteractor by inject()
    private val observerBus: ObserverBus by inject()

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onDatabaseStatusChanged(status: DatabaseStatus) {
        if (isRunning()) {
            val message = getMessageByDatabaseStatus(status)
            start(message)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate:")
        observerBus.register(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = intent?.getParcelableExtra<LockServiceCommand>(EXTRA_COMMAND)
            ?: return START_NOT_STICKY

        Timber.d("onStartCommand: %s", command::class.java.simpleName)

        processCommand(command)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy: state=%s", getCurrentState())
        observerBus.unregister(this)
        job.cancel()
        if (isRunning()) {
            stop(callStopSelf = false)
        }
    }

    private fun processCommand(command: LockServiceCommand) {
        when (command) {
            is LockServiceCommand.ShowNotification -> {
                runShowNotificationCommand()
            }
            is LockServiceCommand.Stop -> {
                runStopCommand()
            }
            is LockServiceCommand.SyncAndLock -> {
                runSyncAndLockCommand(command.file)
            }
        }
    }

    private fun runShowNotificationCommand() {
        scope.launch {
            val getStatusResult = interactor.getDatabaseStatus()
            if (getStatusResult.isFailed) {
                if (isRunning()) {
                    stop()
                }
                return@launch
            }

            start(getMessageByDatabaseStatus(getStatusResult.obj))
        }
    }

    private fun runStopCommand() {
        if (isRunning()) {
            stop()
        }
    }

    private fun runSyncAndLockCommand(file: FileDescriptor) {
        scope.launch {
            start(getString(R.string.notification_text_sending))

            val syncResult = interactor.syncAndLock(file)

            Timber.d("syncResult=%s", syncResult)

            if (syncResult.isFailed) {
                if (isRunning()) {
                    stop()
                }
                return@launch
            }

            stop()
        }
    }

    private fun start(message: String) {
        state.set(ServiceState.STARTED)
        startForeground(LOCK_NOTIFICATION_ID, createNotification(message))
    }

    private fun stop(callStopSelf: Boolean = true) {
        state.set(ServiceState.STOPPED)
        stopForeground(true)

        if (callStopSelf) {
            stopSelf()
        }
    }

    private fun createNotification(message: String): Notification {
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

        val builder = NotificationCompat.Builder(this, getChannelId())
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_lock_open_white_24dp)
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_lock_grey_600_24dp,
                getString(R.string.lock_database),
                actionIntent
            )

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

    private fun isRunning() = (getCurrentState() != ServiceState.STOPPED)

    private fun getMessageByDatabaseStatus(status: DatabaseStatus): String {
        return if (status == DatabaseStatus.DELAYED_CHANGES) {
            getString(R.string.notification_text_unsent_changes)
        } else {
            getString(R.string.notification_text_normal)
        }
    }

    companion object {

        private const val LOCK_NOTIFICATION_ID = 1
        private const val EXTRA_COMMAND = "command"

        private val state = AtomicReference<ServiceState>()

        fun getCurrentState(): ServiceState =
            state.get() ?: ServiceState.STOPPED

        fun runCommand(context: Context, command: LockServiceCommand) {
            if (getCurrentState() == ServiceState.STOPPED) {
                state.set(ServiceState.AWAITING_FOR_START)
            }

            context.startService(createIntent(context, command))
        }

        private fun createIntent(
            context: Context,
            command: LockServiceCommand?
        ): Intent {
            return Intent(context, LockService::class.java)
                .apply {
                    if (command != null) {
                        putExtra(EXTRA_COMMAND, command)
                    }
                }
        }
    }
}