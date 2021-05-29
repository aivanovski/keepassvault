package com.ivanovsky.passnotes.presentation.core

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import okhttp3.internal.toHexString

class DatabaseInteractionWatcher(fragment: Fragment) : LifecycleObserver {

    private val lockInteractor: DatabaseLockInteractor by inject()
    private val screenKey = fragment::class.java.name + "_" + fragment.hashCode().toHexString()

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun notifyOnScreenInteractionStarted() {
        lockInteractor.onScreenInteractionStarted(screenKey)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun notifyOnScreenInteractionStopped() {
        lockInteractor.onScreenInteractionStopped(screenKey)
    }
}