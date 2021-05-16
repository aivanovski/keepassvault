package com.ivanovsky.passnotes.presentation.core.event

interface EventProvider {

    fun subscribe(subscriber: Any, observer: (event: Event) -> Unit)
    fun unSubscribe(subscriber: Any)
    fun send(event: Event)
    fun clear()
}