package com.ivanovsky.passnotes.presentation.core.navigation

import androidx.lifecycle.LifecycleOwner

interface RouterProvider {
    fun getRouter(): Router
    fun bind(owner: LifecycleOwner, host: NavigationHost)
}

class RouterProviderImpl : RouterProvider {

    private val navigator = NavigatorImpl()
    private val router = RouterImpl(navigator = navigator)

    override fun getRouter(): Router = router
    override fun bind(owner: LifecycleOwner, host: NavigationHost) = navigator.bind(owner, host)
}

