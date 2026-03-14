package com.ivanovsky.passnotes.presentation.core.navigation

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import kotlin.reflect.KClass

interface Navigator : Router {
    fun bind(owner: LifecycleOwner, host: NavigationHost)
}

class NavigatorImpl : Navigator {

    private val events = SingleLiveEvent<NavigationEvent>()
    private var currentHost: NavigationHost? = null

    override fun bind(
        owner: LifecycleOwner,
        host: NavigationHost
    ) {
        events.observe(owner) { event ->
            host.handleEvent(event)
        }

        currentHost = host

        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                currentHost = null
            }
        })
    }

    override fun setRoot(vararg screens: Screen) {
        events.send(NavigationEvent.SetRoot(screens.toList()))
    }

    override fun navigateTo(screen: Screen) {
        events.send(NavigationEvent.NavigateTo(screen))
    }

    override fun replaceCurrent(screen: Screen) {
        events.send(NavigationEvent.ReplaceCurrent(screen))
    }

    override fun navigateBack() {
        events.send(NavigationEvent.Back)
    }

    override fun backTo(screen: Screen) {
        events.send(NavigationEvent.BackTo(screen))
    }

    override fun setResultListener(
        screenType: KClass<out Screen>,
        onResult: ResultListener
    ) {
        currentHost?.setResultListener(screenType, onResult)
    }

    override fun setResult(
        screenType: KClass<out Screen>,
        result: Any
    ) {
        currentHost?.setResult(screenType, result)
    }
}