package com.ivanovsky.passnotes.domain.globalsnackbar

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.ivanovsky.passnotes.presentation.Screen
import com.ivanovsky.passnotes.presentation.core.SnackbarMessage
import java.util.concurrent.atomic.AtomicBoolean

class GlobalSnackbarMessageLiveAction : MutableLiveData<FilterableSnackbarMessage>() {

    private val pending = AtomicBoolean(false)

    fun observe(
        owner: LifecycleOwner,
        screen: Screen,
        observer: Observer<SnackbarMessage>
    ) {
        super.observe(owner, Observer { action ->
            if (action.isAcceptableForScreen(screen) && pending.compareAndSet(true, false)) {
                observer.onChanged(action)
            }
        })
    }

    @MainThread
    override fun observe(
        owner: LifecycleOwner,
        observer: Observer<in FilterableSnackbarMessage>
    ) {
        throw IllegalStateException("method invocation is forbidden, use observe(owner, screen, observer)")
    }

    @MainThread
    override fun setValue(t: FilterableSnackbarMessage?) {
        pending.set(true)
        super.setValue(t)
    }

    @MainThread
    fun call(value: FilterableSnackbarMessage?) {
        setValue(value)
    }
}
