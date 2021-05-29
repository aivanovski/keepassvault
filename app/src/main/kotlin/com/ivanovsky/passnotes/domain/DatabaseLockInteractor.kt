package com.ivanovsky.passnotes.domain

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.UiThread
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.repository.SettingsRepository
import com.ivanovsky.passnotes.domain.entity.ServiceState
import com.ivanovsky.passnotes.domain.usecases.DatabaseLockUseCase
import com.ivanovsky.passnotes.presentation.service.DatabaseLockService
import com.ivanovsky.passnotes.util.Logger

class DatabaseLockInteractor(
    private val context: Context,
    private val settings: SettingsRepository,
    private val lockUseCase: DatabaseLockUseCase
) : ObserverBus.DatabaseOpenObserver,
    ObserverBus.DatabaseCloseObserver {

    private val handler = Handler(Looper.getMainLooper())
    private val activeScreens = mutableSetOf<String>()
    private var isDatabaseOpened = false
    private var isTimerStarted = false

    @UiThread
    override fun onDatabaseOpened() {
        startServiceIfNeed()
        isDatabaseOpened = true
    }

    @UiThread
    override fun onDatabaseClosed() {
        cancelLockTimer()
        stopServiceIfNeed()
        isDatabaseOpened = false
    }

    @UiThread
    fun onScreenInteractionStarted(screenKey: String) {
        activeScreens.add(screenKey)
        cancelLockTimer()
    }

    @UiThread
    fun onScreenInteractionStopped(screenKey: String) {
        activeScreens.remove(screenKey)

        if (isDatabaseOpened &&
            activeScreens.size == 0 &&
            settings.autoLockDelayInMs != null) {
            startLockTimer()
        }
    }

    private fun startServiceIfNeed() {
        if (DatabaseLockService.getCurrentState() == ServiceState.STOPPED &&
            settings.isLockNotificationVisible) {
            DatabaseLockService.start(context)
        }
    }

    private fun stopServiceIfNeed() {
        if (DatabaseLockService.getCurrentState() != ServiceState.STOPPED) {
            DatabaseLockService.stop(context)
        }
    }

    private fun cancelLockTimer() {
        if (isTimerStarted) {
            isTimerStarted = false
            Logger.d(TAG, "Auto-Lock timer cancelled")
        }

        handler.removeCallbacksAndMessages(null)
    }

    private fun startLockTimer() {
        val delay = settings.autoLockDelayInMs ?: return

        isTimerStarted = true
        handler.postDelayed(
            {
                isTimerStarted = false
                Logger.d(TAG, "Lock database by timer")
                lockUseCase.lockIfNeed()
            },
            delay.toLong()
        )

        Logger.d(TAG, "Auto-Lock timer started with delay: %s milliseconds", delay)
    }

    companion object {
        private val TAG = DatabaseLockInteractor::class.simpleName
    }
}