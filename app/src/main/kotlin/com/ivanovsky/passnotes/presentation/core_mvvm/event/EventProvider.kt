package com.ivanovsky.passnotes.presentation.core_mvvm.event

import android.os.Bundle

interface EventProvider {

    fun subscribe(subscriber: Any, observer: (event: Bundle) -> Unit)
    fun unSubscribe(subscriber: Any)
    fun send(event: Bundle)
    fun clear()
}