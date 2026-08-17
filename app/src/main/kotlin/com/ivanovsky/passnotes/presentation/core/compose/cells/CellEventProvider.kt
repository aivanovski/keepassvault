package com.ivanovsky.passnotes.presentation.core.compose.cells

import java.util.concurrent.ConcurrentHashMap

interface CellEventProvider {

    fun subscribe(
        subscriber: Any,
        listener: EventListener
    )

    fun unsubscribe(subscriber: Any)
    fun sendEvent(event: CellEvent)
    fun isSubscribed(subscriber: Any): Boolean
    fun clear()
}

fun interface EventListener {
    fun onEvent(event: CellEvent)
}

class CellEventProviderImpl : CellEventProvider {

    private val listenerBySubscriberType: MutableMap<String, EventListener> = ConcurrentHashMap()

    override fun subscribe(
        subscriber: Any,
        listener: EventListener
    ) {
        listenerBySubscriberType[subscriber.key()] = listener
    }

    override fun unsubscribe(subscriber: Any) {
        listenerBySubscriberType.remove(subscriber.key())
    }

    override fun sendEvent(event: CellEvent) {
        for (listener in listenerBySubscriberType.values) {
            listener.onEvent(event)
        }
    }

    override fun isSubscribed(subscriber: Any): Boolean {
        return listenerBySubscriberType.containsKey(subscriber.key())
    }

    override fun clear() {
        listenerBySubscriberType.clear()
    }

    private fun Any.key(): String {
        return this::class.java.name
    }
}