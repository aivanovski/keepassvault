package com.ivanovsky.passnotes.domain.interactor.syncState

import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.presentation.syncState.model.SyncStateModel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SyncStateCache(
    dispatchers: DispatcherProvider
) {

    private val listeners = CopyOnWriteArrayList<OnSyncStateChangeListener>()
    private val scope = CoroutineScope(dispatchers.Main)
    private val value = AtomicReference<SyncStateModel>()

    fun getValue(): SyncStateModel? = value.get()

    fun setValue(newValue: SyncStateModel?) {
        value.set(newValue)
        notifyListeners()
    }

    fun subscribe(listener: OnSyncStateChangeListener) {
        listeners.add(listener)
    }

    fun unsubscribe(listener: OnSyncStateChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        val model = getValue() ?: return

        scope.launch {
            listeners.forEach { listener -> listener.onSyncStateCacheChanged(model) }
        }
    }

    fun clear() {
        listeners.clear()
    }

    fun interface OnSyncStateChangeListener {
        fun onSyncStateCacheChanged(model: SyncStateModel)
    }
}