package com.ivanovsky.passnotes.domain

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.UiThread
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.usecases.LockDatabaseUseCase
import com.ivanovsky.passnotes.presentation.service.LockService
import com.ivanovsky.passnotes.presentation.service.model.LockServiceCommand
import com.ivanovsky.passnotes.presentation.service.model.ServiceState
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

class DatabaseLockInteractor(
    private val context: Context,
    private val settings: Settings,
    private val lockUseCase: LockDatabaseUseCase,
    private val clipboardInteractor: ClipboardInteractor
) {

    private val handler = Handler(Looper.getMainLooper())
    private val activeScreens = CopyOnWriteArrayList<String>()
    private val isDatabaseOpened = AtomicBoolean(false)
    private val isTimerStarted = AtomicBoolean(false)

    fun isDatabaseOpened(): Boolean = isDatabaseOpened.get()

    fun onDatabaseOpened(db: EncryptedDatabase) {
        startServiceIfNeed(db)
        isDatabaseOpened.set(true)
        if (settings.autoLockDelayInMs != -1) {
            startLockTimer()
        }
    }

    fun onDatabaseClosed() {
        cancelLockTimer()
        stopServiceIfNeed()
        isDatabaseOpened.set(false)
        clipboardInteractor.clearIfNeed()
    }

    @UiThread
    fun onScreenInteractionStarted(screenKey: String) {
        Timber.d("onScreenInteractionStarted: screenKey=%s", screenKey)
        activeScreens.add(screenKey)
        cancelLockTimer()
    }

    @UiThread
    fun onScreenInteractionStopped(screenKey: String) {
        activeScreens.remove(screenKey)

        Timber.d(
            "onScreenInteractionStopped: screenKey=%s, activeScreens=%s",
            screenKey,
            activeScreens.size
        )

        if (isDatabaseOpened.get() &&
            activeScreens.size == 0 &&
            settings.autoLockDelayInMs != -1
        ) {
            startLockTimer()
        }
    }

    fun stopServiceIfNeed() {
        if (LockService.getCurrentState() != ServiceState.STOPPED) {
            Timber.d("stopServiceIfNeed:")
            LockService.runCommand(context, LockServiceCommand.Stop)
        }
    }

    fun invalidateNotificationIfNeed() {
        Timber.d("invalidateNotificationIfNeed:")
        if (LockService.getCurrentState() != ServiceState.STOPPED) {
            LockService.runCommand(context, LockServiceCommand.ShowNotification)
        }
    }

    private fun startServiceIfNeed(db: EncryptedDatabase) {
        val shouldShowNotification =
            (settings.isLockNotificationVisible || db.fsOptions.isPostponedSyncEnabled)
        val shouldStart = (
            LockService.getCurrentState() == ServiceState.STOPPED &&
                shouldShowNotification
            )
        Timber.d("startServiceIfNeed: shouldStart=%s", shouldStart)

        if (shouldStart) {
            LockService.runCommand(context, LockServiceCommand.ShowNotification)
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
}