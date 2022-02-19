package com.ivanovsky.passnotes.injection

import org.koin.core.context.GlobalContext
import org.koin.core.parameter.DefinitionParameters

object GlobalInjector {

    inline fun <reified T : Any> inject(): Lazy<T> = GlobalContext.get().koin.inject(null, null)

    inline fun <reified T : Any> get(
        params: DefinitionParameters? = null
    ): T = GlobalContext.get().koin.get(
        null,
        parameters = if (params != null) {
            { params }
        } else {
            null
        }
    )
}