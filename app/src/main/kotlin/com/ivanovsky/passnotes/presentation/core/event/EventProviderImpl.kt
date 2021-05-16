package com.ivanovsky.passnotes.presentation.core.event

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.jvm.jvmName

class EventProviderImpl : EventProvider {

    private val subscribers = ConcurrentHashMap<String, (event: Event) -> Unit>()

    override fun subscribe(subscriber: Any, observer: (event: Event) -> Unit) {
        val key = subscriber::class.jvmName

        subscribers[key] = observer
    }

    override fun unSubscribe(subscriber: Any) {
        val key = subscriber::class.jvmName
        subscribers.remove(key)
    }

    override fun send(event: Event) {
        subscribers.values.forEach { observer -> observer.invoke(event) }
    }

    override fun clear() {
        subscribers.clear()
    }
}