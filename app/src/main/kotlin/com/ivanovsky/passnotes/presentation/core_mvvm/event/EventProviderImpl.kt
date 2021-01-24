package com.ivanovsky.passnotes.presentation.core_mvvm.event

import android.os.Bundle
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.jvm.jvmName

class EventProviderImpl : EventProvider {

    private val subscribers = ConcurrentHashMap<String, (event: Bundle) -> Unit>()

    override fun subscribe(subscriber: Any, observer: (event: Bundle) -> Unit) {
        val key = subscriber::class.jvmName

        subscribers[key] = observer
    }

    override fun unSubscribe(subscriber: Any) {
        val key = subscriber::class.jvmName
        subscribers.remove(key)
    }

    override fun send(event: Bundle) {
        subscribers.values.forEach { observer -> observer.invoke(event) }
    }

    override fun clear() {
        subscribers.clear()
    }
}