package com.ivanovsky.passnotes.injection

import org.koin.core.context.GlobalContext

object GlobalInjector {
    inline fun <reified T : Any> inject(): Lazy<T> = GlobalContext.get().koin.inject(null, null)
}