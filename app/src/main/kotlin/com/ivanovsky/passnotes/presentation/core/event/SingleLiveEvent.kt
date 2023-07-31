package com.ivanovsky.passnotes.presentation.core.event

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

open class SingleLiveEvent<T> : MutableLiveData<T>() {

    private val pending = AtomicBoolean(false)

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(
            owner,
            Observer { value ->
                if (pending.compareAndSet(true, false)) {
                    observer.onChanged(value)
                }
            }
        )
    }

    override fun setValue(value: T?) {
        pending.set(true)
        super.setValue(value)
    }

    fun call(value: T) {
        this.value = value
    }
}