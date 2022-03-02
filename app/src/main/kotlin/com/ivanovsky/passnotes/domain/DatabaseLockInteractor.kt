package com.ivanovsky.passnotes.domain

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.UiThread
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.presentation.service.model.ServiceState
import com.ivanovsky.passnotes.domain.usecases.LockDatabaseUseCase
import com.ivanovsky.passnotes.presentation.service.LockService
import com.ivanovsky.passnotes.presentation.service.model.LockServiceCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class DatabaseLockInteractor(
    private val context: Context,
    private val settings: Settings,
    private val lockUseCase: LockDatabaseUseCase
)  {

    private val handler = Handler(Looper.getMainLooper())
    private val activeScreens = CopyOnWriteArrayList<String>()
    private val isDatabaseOpened = AtomicBoolean(false)
    private val isTimerStarted = AtomicBoolean(false)
    private val databaseFsOptions = AtomicReference<FSOptions>()
    private val databaseStatus = AtomicReference<DatabaseStatus>()

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun onDatabaseOpened(fsOptions: FSOptions, status: DatabaseStatus) {
        startServiceIfNeed()
        isDatabaseOpened.set(true)
        databaseFsOptions.set(fsOptions)
        databaseStatus.set(status)
    }

    fun onDatabaseClosed() {
        cancelLockTimer()
        stopServiceIfNeed()
        isDatabaseOpened.set(false)
        databaseFsOptions.set(null)
        databaseStatus.set(null)
    }

    @UiThread
    fun onScreenInteractionStarted(screenKey: String) {
        activeScreens.add(screenKey)
        cancelLockTimer()
    }

    @UiThread
    fun onScreenInteractionStopped(screenKey: String) {
        activeScreens.remove(screenKey)

        if (isDatabaseOpened.get() &&
            activeScreens.size == 0 &&
            settings.autoLockDelayInMs != -1) {
            startLockTimer()
        }
    }

    fun stopServiceIfNeed() {
        if (LockService.getCurrentState() != ServiceState.STOPPED) {
            Timber.d("stopServiceIfNeed:")
            scope.launch {
                LockService.runCommand(context, LockServiceCommand.Stop)
            }
        }
    }

    private fun startServiceIfNeed() {
        val status = databaseStatus.get()
        if (LockService.getCurrentState() == ServiceState.STOPPED &&
            (settings.isLockNotificationVisible || status == DatabaseStatus.DELAYED_CHANGES)) {
            scope.launch {
                LockService.runCommand(context, LockServiceCommand.ShowNotification)
            }
        }
    }

    private fun cancelLockTimer() {
        if (isTimerStarted.get()) {
            isTimerStarted.set(false)
            Timber.d("Auto-Lock timer cancelled")
        }

        handler.removeCallbacksAndMessages(null)
    }

    private fun startLockTimer() {
        val delay = settings.autoLockDelayInMs

        isTimerStarted.set(true)
        handler.postDelayed(
            {
                isTimerStarted.set(false)
                Timber.d("Lock database by timer")
                lockUseCase.lockIfNeed()
            },
            delay.toLong()
        )

        Timber.d("Auto-Lock timer started with delay: %s milliseconds", delay)
    }

    companion object {
        private val TAG = DatabaseLockInteractor::class.simpleName
    }
}