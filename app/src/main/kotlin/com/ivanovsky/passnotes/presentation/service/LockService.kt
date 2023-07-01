package com.ivanovsky.passnotes.presentation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.domain.interactor.service.LockServiceInteractor
import com.ivanovsky.passnotes.extensions.getNotificationManager
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.main.MainActivity
import com.ivanovsky.passnotes.presentation.main.MainScreenArgs
import com.ivanovsky.passnotes.presentation.service.model.LockServiceCommand
import com.ivanovsky.passnotes.presentation.service.model.ServiceState
import com.ivanovsky.passnotes.presentation.service.task.LockServiceTask
import com.ivanovsky.passnotes.presentation.service.task.ShowNotificationTask
import com.ivanovsky.passnotes.presentation.service.task.StopServiceTask
import com.ivanovsky.passnotes.presentation.service.task.SyncAndLockTask
import com.ivanovsky.passnotes.util.IntentUtils.defaultPendingIntentFlags
import com.ivanovsky.passnotes.util.getParcelable
import java.util.concurrent.atomic.AtomicReference
import timber.log.Timber

class LockService : Service(),
    ObserverBus.DatabaseStatusObserver,
    LockServiceFacade {

    private val interactor: LockServiceInteractor by inject()
    private val observerBus: ObserverBus by inject()
    private val dispatchers: DispatcherProvider by inject()
    private val resourceProvider: ResourceProvider by inject()

    private val taskProcessor = LockServiceTaskProcessor(this, dispatchers)

    override fun onDatabaseStatusChanged(status: DatabaseStatus) {
        if (isRunning()) {
            val message = getMessageByDatabaseStatus(status)
            showNotification(message)
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
        val command = intent?.getParcelable(EXTRA_COMMAND, LockServiceCommand::class.java)
            ?: return START_NOT_STICKY

        Timber.d("onStartCommand: %s", command::class.java.simpleName)

        state.set(ServiceState.STARTED)
        taskProcessor.process(command.toTask())

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy: state=%s", getCurrentState())
        taskProcessor.stop()
        observerBus.unregister(this)
        if (isRunning()) {
            stop(callStopSelf = false)
        }
    }

    override fun stop() {
        stop(callStopSelf = true)
    }

    override fun showNotification(message: String) {
        startForeground(LOCK_NOTIFICATION_ID, createNotification(message))
    }

    override fun hideNotification() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun stop(callStopSelf: Boolean = true) {
        state.set(ServiceState.STOPPED)
        stopForeground(STOP_FOREGROUND_REMOVE)

        if (callStopSelf) {
            stopSelf()
        }
    }

    private fun createNotification(message: String): Notification {
        val channel = NotificationChannel(
            getChannelId(),
            getString(R.string.lock_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.lock_notification_channel_description)
        }

        getNotificationManager().createNotificationChannel(channel)

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            createContentIntent(),
            defaultPendingIntentFlags()
        )
        val actionIntent = PendingIntent.getBroadcast(
            this,
            0,
            createLockButtonIntent(),
            defaultPendingIntentFlags()
        )

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
        return MainActivity.createStartIntent(
            this,
            MainScreenArgs(
                appMode = ApplicationLaunchMode.NORMAL
            )
        )
    }

    private fun createLockButtonIntent(): Intent {
        return Intent(this, DatabaseLockBroadcastReceiver::class.java)
    }

    private fun getChannelId(): String {
        return applicationContext.packageName + "_notification_channel"
    }

    private fun isRunning() = (getCurrentState() != ServiceState.STOPPED)

    private fun getMessageByDatabaseStatus(status: DatabaseStatus): String {
        return if (status == DatabaseStatus.POSTPONED_CHANGES) {
            getString(R.string.lock_notification_text_not_synchronized)
        } else {
            getString(R.string.lock_notification_text_normal)
        }
    }

    private fun LockServiceCommand.toTask(): LockServiceTask {
        return when (this) {
            is LockServiceCommand.ShowNotification -> {
                ShowNotificationTask(interactor, resourceProvider)
            }

            is LockServiceCommand.Stop -> {
                StopServiceTask()
            }

            is LockServiceCommand.SyncAndLock -> {
                SyncAndLockTask(interactor, resourceProvider, file)
            }
        }
    }

    companion object {

        private const val LOCK_NOTIFICATION_ID = 1
        private const val EXTRA_COMMAND = "command"

        private val state = AtomicReference(ServiceState.STOPPED)

        fun getCurrentState(): ServiceState = state.get()

        fun runCommand(context: Context, command: LockServiceCommand) {
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