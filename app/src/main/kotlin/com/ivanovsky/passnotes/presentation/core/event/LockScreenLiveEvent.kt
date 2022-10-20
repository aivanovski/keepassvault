package com.ivanovsky.passnotes.presentation.core.event

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor

class LockScreenLiveEvent(
    private val observerBus: ObserverBus,
    private val lockInteractor: DatabaseLockInteractor
) : SingleLiveEvent<Unit>(),
    ObserverBus.DatabaseCloseObserver {

    private var isRegistered = false

    override fun onActive() {
        super.onActive()
        if (lockInteractor.isDatabaseOpened()) {
            observerBus.register(this)
            isRegistered = true
        } else {
            onDatabaseClosed()
        }
    }

    override fun onInactive() {
        super.onInactive()
        if (isRegistered) {
            observerBus.unregister(this)
            isRegistered = false
        }
    }

    override fun onDatabaseClosed() {
        call(Unit)
    }
}